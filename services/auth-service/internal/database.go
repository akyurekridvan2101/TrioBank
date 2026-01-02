package internal

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"strings"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/TrioBank/triobank-platform/microservices/auth-service/pkg"
	"github.com/redis/go-redis/v9"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"
)

type MongoDB struct {
	Db *mongo.Database
}
type RedisDB struct {
	Db *redis.Client
}

func (db MongoDB) loginControl(ctx context.Context, data loginData) (User, error) {
	var user User
	// Normalize TC for query (trim whitespace)
	tc := strings.TrimSpace(data.Tc)
	collection := db.Db.Collection("Users").FindOne(ctx, bson.M{"tc": tc})
	err := collection.Decode(&user)
	if err != nil {
		return user, err
	}
	err = pkg.HashedPasswordControl(data.Password, user.HashedPassword)
	if err != nil {
		return user, err
	} else {
		return user, nil
	}
}

func (db MongoDB) createRefreshAndAccessToken(ctx context.Context, userId primitive.ObjectID, userUUID string) (string, string, error) {
	collection := db.Db.Collection("Tokens")
	_, err := collection.UpdateMany(ctx, bson.M{"user_id": userId}, bson.M{"$set": bson.M{"isActive": false}})
	if err != nil {
		return "", "", err
	}

	var refresh Tokens
	refresh.Id = primitive.NewObjectID()
	refresh.UserId = userId
	refresh.UserUUID = userUUID
	refresh.IsActive = true
	refresh.CreatedAt = time.Now()
	refresh.ExpiredAt = time.Now().Add(time.Hour * 7 * 24)

	refreshToken, err := pkg.CreateRefreshToken(userUUID)
	if err != nil {
		return "", "", err
	}
	refresh.Token = refreshToken

	accessToken, err := pkg.CreateAccessToken(userUUID)
	if err != nil {
		return "", "", err
	}

	_, err = collection.InsertOne(ctx, refresh)
	if err != nil {
		return "", "", err
	}
	return refreshToken, accessToken, nil
}
func (db MongoDB) createAccessToken(ctx context.Context, refreshToken string) (string, error) {
	userUUID, err := db.isRefreshTokenExistAndActive(ctx, refreshToken)
	if err != nil {
		return "", err
	}
	accessToken, err := pkg.CreateAccessToken(userUUID)
	if err != nil {
		return "", err
	}
	return accessToken, nil
}
func (db MongoDB) inActiveRefreshToken(ctx context.Context, refreshToken string) error {
	collection := db.Db.Collection("Tokens")
	_, err := collection.UpdateOne(ctx, bson.M{"token": refreshToken, "isActive": true}, bson.M{"$set": bson.M{"isActive": false}})
	return err
}

func (db MongoDB) isRefreshTokenExistAndActive(ctx context.Context, refreshToken string) (string, error) {
	collection := db.Db.Collection("Tokens")
	var tempToken Tokens
	result := collection.FindOne(ctx, bson.M{"token": refreshToken})
	if errors.Is(result.Err(), mongo.ErrNoDocuments) {
		return "", ErrTokenIsNotExist
	}
	err := result.Decode(&tempToken)
	if err != nil {
		return "", err
	}
	if tempToken.IsActive != true {
		return "", ErrTokenIsNotActive
	}
	if !tempToken.ExpiredAt.After(time.Now()) {
		_ = db.inActiveRefreshToken(ctx, refreshToken)
		return "", ErrTokenExpired
	}
	return tempToken.UserUUID, nil
}

func (db MongoDB) isUserExist(ctx context.Context, tc string) error {
	collection := db.Db.Collection("Users")
	// Normalize TC for query (trim whitespace)
	tc = strings.TrimSpace(tc)
	result := collection.FindOne(ctx, bson.M{"tc": tc})
	if errors.Is(result.Err(), mongo.ErrNoDocuments) {
		return nil
	}
	if result.Err() != nil {
		return result.Err()
	}
	return ErrUserAlreadyExist
}

func (db MongoDB) createUser(ctx context.Context, user User) error {
	collection := db.Db.Collection("Users")
	_, err := collection.InsertOne(ctx, user)
	if err != nil {
		return err
	}
	return nil
}
func (db MongoDB) validateUserPassword(ctx context.Context, userUuid, password string) (primitive.ObjectID, error) {
	collection := db.Db.Collection("Users")
	var user User
	err := collection.FindOne(ctx, bson.M{"uuid": userUuid}).Decode(&user)
	if err != nil {
		return primitive.NilObjectID, err
	}

	if err := pkg.HashedPasswordControl(password, user.HashedPassword); err != nil {
		return primitive.NilObjectID, ErrOldPasswordInvalid
	}
	return user.Id, nil
}
func (db MongoDB) updatePassword(ctx context.Context, userId primitive.ObjectID, newPassword string) error {
	collection := db.Db.Collection("Users")
	_, err := collection.UpdateOne(ctx, bson.M{"_id": userId}, bson.M{"$set": bson.M{"hashedPassword": newPassword}})
	return err
}

func (db MongoDB) updateUserContact(ctx context.Context, userId primitive.ObjectID, email, phone string) error {
	collection := db.Db.Collection("Users")
	update := bson.M{}
	if email != "" {
		// Normalize email: trim and lowercase
		update["email"] = strings.TrimSpace(strings.ToLower(email))
	}
	if phone != "" {
		// Normalize phone: trim
		update["tel"] = strings.TrimSpace(phone)
	}
	if len(update) == 0 {
		return nil
	}
	_, err := collection.UpdateOne(ctx, bson.M{"_id": userId}, bson.M{"$set": update})
	return err
}

func (db MongoDB) getUserIdByUUID(ctx context.Context, uuid string) (primitive.ObjectID, error) {
	collection := db.Db.Collection("Users")
	var user User
	err := collection.FindOne(ctx, bson.M{"uuid": uuid}).Decode(&user)
	if err != nil {
		return primitive.NilObjectID, err
	}
	return user.Id, nil
}

func (db MongoDB) verifyUserEmail(ctx context.Context, tc string, email string) (User, error) {
	var user User
	// Normalize input: T.C. Kimlik No'yu trim et
	tc = strings.TrimSpace(tc)
	// Email'i normalize et: trim ve lowercase
	email = strings.TrimSpace(strings.ToLower(email))
	
	// MongoDB'de TC ile kullanıcıyı bul
	// Önce normalize edilmiş TC ile dene
	collection := db.Db.Collection("Users").FindOne(ctx, bson.M{"tc": tc})
	err := collection.Decode(&user)
	
	// Eğer bulunamazsa, eski kayıtlar için boşluklu TC ile de ara
	if err != nil && errors.Is(err, mongo.ErrNoDocuments) {
		// TC'nin başında/sonunda boşluk olabilir, regex ile ara
		// TC sadece rakam olduğu için regex güvenli
		regexTc := "^\\s*" + tc + "\\s*$"
		collection = db.Db.Collection("Users").FindOne(ctx, bson.M{"tc": bson.M{"$regex": regexTc}})
		err = collection.Decode(&user)
	if err != nil {
			return user, err
		}
		// Bulunan kullanıcının TC'sini normalize et (veritabanını düzelt)
		if strings.TrimSpace(user.Tc) != user.Tc {
			// Veritabanındaki TC'yi normalize et
			normalizedTc := strings.TrimSpace(user.Tc)
			_, updateErr := db.Db.Collection("Users").UpdateOne(ctx, bson.M{"_id": user.Id}, bson.M{"$set": bson.M{"tc": normalizedTc}})
			if updateErr == nil {
				user.Tc = normalizedTc
			}
		}
	} else if err != nil {
		return user, err
	}
	
	// Email eşleşmesini case-insensitive ve trim-aware kontrol et
	userEmailNormalized := strings.TrimSpace(strings.ToLower(user.Email))
	if userEmailNormalized != email {
		return user, errors.New("email does not match")
	}
	
	// Email'i de normalize et (veritabanını düzelt)
	if userEmailNormalized != user.Email {
		_, updateErr := db.Db.Collection("Users").UpdateOne(ctx, bson.M{"_id": user.Id}, bson.M{"$set": bson.M{"email": userEmailNormalized}})
		if updateErr == nil {
			user.Email = userEmailNormalized
		}
	}
	
	return user, nil
}

func (db MongoDB) deactivateAllRefreshTokens(ctx context.Context, userId primitive.ObjectID) error {
	collection := db.Db.Collection("Tokens")
	_, err := collection.UpdateMany(ctx, bson.M{"user_id": userId}, bson.M{"$set": bson.M{"isActive": false}})
	return err
}

func (db MongoDB) deleteUser(ctx context.Context, userId primitive.ObjectID) error {
	collection := db.Db.Collection("Users")
	result, err := collection.DeleteOne(ctx, bson.M{"_id": userId})
	if err != nil {
		return err
	}
	if result.DeletedCount == 0 {
		return fmt.Errorf("user not found with id: %s", userId.String())
	}
	return nil
}

func (db MongoDB) getUserById(ctx context.Context, userId primitive.ObjectID) (User, error) {
	collection := db.Db.Collection("Users")
	var user User
	result := collection.FindOne(ctx, bson.M{"_id": userId})
	if result.Err() != nil {
		return User{}, result.Err()
	}
	err := result.Decode(&user)
	if err != nil {
		return User{}, err
	}
	return user, nil
}

func (db RedisDB) saveSessionId(ctx context.Context, userId primitive.ObjectID, sessionId string, code int64) error {
	var value RedisSessionData
	value.UserId = userId
	value.Code = code

	v, err := json.Marshal(value)
	if err != nil {
		return err
	}
	_, err = db.Db.Set(ctx, sessionId, v, time.Minute*3).Result()
	if err != nil {
		return err
	} else {
		return nil
	}
}
func (db RedisDB) deleteSessionId(ctx context.Context, sessionId string) error {
	_, err := db.Db.Del(ctx, sessionId).Result()
	if err != nil {
		return err
	}
	return nil
}
func (db RedisDB) controlSessionId(ctx context.Context, sessionId string, code int64) (primitive.ObjectID, error) {
	var value RedisSessionData
	respond := db.Db.Get(ctx, sessionId)
	result, err := respond.Bytes()
	if errors.Is(err, redis.Nil) {
		return primitive.ObjectID{}, ErrCodeNotFound
	}
	if err != nil {
		return primitive.ObjectID{}, err
	}
	err = json.Unmarshal(result, &value)
	if err != nil {
		return primitive.ObjectID{}, err
	}
	if value.Code != code {
		return primitive.ObjectID{}, ErrCodeIsNotCorrect
	}
	return value.UserId, nil
}
func (db RedisDB) setAndControlLimitById(ctx context.Context, userId primitive.ObjectID) (bool, error) {
	key := "limit:" + userId.String()
	result, err := db.Db.SetNX(ctx, key, "limited", 3*time.Minute).Result()
	return result, err
}
func (db RedisDB) setAndControlLimitByTc(ctx context.Context, tc string) (bool, error) {
	key := "limit:" + tc
	result, err := db.Db.SetNX(ctx, key, "limited", 3*time.Minute).Result()
	return result, err
}

func (db RedisDB) removeLimitById(ctx context.Context, userId primitive.ObjectID) error {
	key := "limit:" + userId.String()
	_, err := db.Db.Del(ctx, key).Result()
	if err != nil {
		return err
	}
	return nil
}
func (db RedisDB) removeLimitByTc(ctx context.Context, tc string) error {
	key := "limit:" + tc
	_, err := db.Db.Del(ctx, key).Result()
	if err != nil {
		return err
	}
	return nil
}

func (db RedisDB) saveUser(ctx context.Context, user User) error {
	key := "user:" + user.Id.String()
	value, err := json.Marshal(user)
	if err != nil {
		return err
	}
	_, err = db.Db.Set(ctx, key, value, 4*time.Minute).Result()
	if err != nil {
		return err
	}
	return nil
}

func (db RedisDB) getUser(ctx context.Context, userId primitive.ObjectID) (User, error) {
	key := "user:" + userId.String()
	var user User
	result, err := db.Db.Get(ctx, key).Bytes()
	if err != nil {
		return User{}, err
	}
	err = json.Unmarshal(result, &user)
	if err != nil {
		return User{}, err
	}
	return user, nil
}

func StartMongoDB() *mongo.Database {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*3)
	defer cancel()

	mongoURI := config.GetEnv("MONGO_URI")
	mongoUsername := config.GetEnv("MONGO_USERNAME")
	mongoPassword := config.GetEnv("MONGO_PASSWORD")

	if mongoUsername != "" && mongoPassword != "" {
		mongoHost := "localhost:27017"

		// MONGO_URI can be in two formats:
		// 1. "mongodb://host:port" (with protocol)
		// 2. "host:port" (without protocol, K8s style)
		if mongoURI != "" {
			if strings.HasPrefix(mongoURI, "mongodb://") {
				// Remove "mongodb://" prefix (10 chars)
				mongoHost = strings.TrimSuffix(mongoURI[10:], "/")
			} else {
				// Use as-is (K8s ConfigMap format)
				mongoHost = strings.TrimSuffix(mongoURI, "/")
			}
		}

		mongoURI = fmt.Sprintf("mongodb://%s:%s@%s", mongoUsername, mongoPassword, mongoHost)
	} else if mongoURI != "" && !strings.HasPrefix(mongoURI, "mongodb://") {
		// No auth but URI doesn't have protocol
		mongoURI = "mongodb://" + mongoURI
	} else if mongoURI == "" {
		mongoURI = "mongodb://localhost:27017"
	}
	client, err := mongo.Connect(ctx, options.Client().ApplyURI(mongoURI))
	if err != nil {
		log.Fatal("mongodb connection error: ", err.Error())
	}
	err = client.Ping(context.Background(), readpref.Primary())
	if err != nil {
		log.Fatal("mongodb ping error: ", err.Error())
	} else {
		fmt.Println("mongodb is running")
	}
	db := client.Database(config.GetEnv("MONGO_NAME"))
	return db
} // mongo database'i baslatir
func StartRedisDB() *redis.Client {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*3)
	defer cancel()
	client := redis.NewClient(&redis.Options{
		Addr:       config.GetEnv("REDIS_PORT"),
		Password:   config.GetEnv("REDIS_PASSWORD"),
		DB:         0,
		ClientName: config.GetEnv("REDIS_NAME"),
	})
	pong, err := client.Ping(ctx).Result()
	if err != nil {
		log.Fatal("redis ping error: ", err.Error())
	} else {
		fmt.Println("redis is running", pong)
	}
	return client
} // redis'i baslatir
