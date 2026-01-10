package cache

import (
	"context"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/api-gateway/config"
	"github.com/redis/go-redis/v9"
)

type RedisClient struct {
	Redis *redis.Client
}

type RedisI interface {
	SetAndControlLimitForProtected(ctx context.Context, userId string, duration time.Duration, maxRequests int64) (bool, error)
	ClearLimitForProtected(ctx context.Context, userId string) error
	SetAndControlLimitForPublic(ctx context.Context, ip string, duration time.Duration, maxRequests int64) (bool, error)
	ClearLimitForPublic(ctx context.Context, ip string) error

	Set(ctx context.Context, key, value string, duration time.Duration) error
	Get(ctx context.Context, key string) (string, error)
}

func StartRedis() *RedisClient {
	client := redis.NewClient(&redis.Options{
		Addr:       config.GetEnv("API_GATEWAY_REDIS_ADDRESS"),
		DB:         0,
		Password:   config.GetEnv("API_GATEWAY_REDIS_PASSWORD"),
		ClientName: config.GetEnv("API_GATEWAY_REDIS_CLIENT_NAME"),
	})
	return &RedisClient{
		Redis: client,
	}
}

// SetAndControlLimitForProtected - User ID bazlı rate limiting (protected endpoints)
// Counter-based: Her istek counter'ı artırır, maxRequests'e ulaşınca engeller
func (r *RedisClient) SetAndControlLimitForProtected(ctx context.Context, userId string, duration time.Duration, maxRequests int64) (bool, error) {
	key := "gateway:ratelimit:user:" + userId

	// Counter'ı artır
	count, err := r.Redis.Incr(ctx, key).Result()
	if err != nil {
		return false, err
	}

	// İlk istek ise expire süresini ayarla
	if count == 1 {
		r.Redis.Expire(ctx, key, duration)
	}

	// Limit kontrolü
	return count <= maxRequests, nil
}

func (r *RedisClient) ClearLimitForProtected(ctx context.Context, userId string) error {
	return r.Redis.Del(ctx, "gateway:ratelimit:user:"+userId).Err()
}

func (r *RedisClient) Get(ctx context.Context, key string) (string, error) {
	return r.Redis.Get(ctx, key).Result()
}

func (r *RedisClient) Set(ctx context.Context, key, value string, duration time.Duration) error {
	return r.Redis.Set(ctx, key, value, duration).Err()
}

// SetAndControlLimitForPublic - IP bazlı rate limiting (public endpoints)
// Counter-based: Her istek counter'ı artırır, maxRequests'e ulaşınca engeller
func (r *RedisClient) SetAndControlLimitForPublic(ctx context.Context, ip string, duration time.Duration, maxRequests int64) (bool, error) {
	key := "gateway:ratelimit:ip:" + ip

	// Counter'ı artır
	count, err := r.Redis.Incr(ctx, key).Result()
	if err != nil {
		return false, err
	}

	// İlk istek ise expire süresini ayarla
	if count == 1 {
		r.Redis.Expire(ctx, key, duration)
	}

	// Limit kontrolü
	return count <= maxRequests, nil
}

func (r *RedisClient) ClearLimitForPublic(ctx context.Context, ip string) error {
	return r.Redis.Del(ctx, "gateway:ratelimit:ip:"+ip).Err()
}
