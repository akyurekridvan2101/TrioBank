package main

import (
	"fmt"
	"math/rand"
	"time"

	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

// Veri modelleri
type UserData struct {
	// Shared
	UserUUID    string
	TCNo        string
	FirstName   string
	LastName    string
	Email       string
	Phone       string
	BirthDate   time.Time
	Password    string // Plain text, will be hashed
	
	// MongoDB fields
	MongoID     primitive.ObjectID
	HashedPassword string
	CreatedAt   time.Time
	
	// Client-service fields
	ClientID    uuid.UUID
	Address     Address
	
	// Account-service fields
	Accounts    []Account
	
	// Card-service fields
	Cards       []Card
	
	// Transaction-service fields
	Transactions []Transaction
	
	// Ledger-service fields
	LedgerEntries []LedgerEntry
}

type Address struct {
	Street     string
	City       string
	District   string
	PostalCode string
}

type Account struct {
	ID            uuid.UUID
	CustomerID    string // UserUUID
	AccountNumber string
	ProductCode   string
	Status        string
	Currency      string
	Balance       float64
	CreatedAt     time.Time
	UpdatedAt     time.Time
}

type Card struct {
	ID              uuid.UUID
	CardType        string // DEBIT, VIRTUAL
	CardNumber      string
	MaskedNumber    string
	CVV             string
	CardholderName  string
	ExpiryMonth     int
	ExpiryYear      int
	CardBrand       string
	Status          string
	AccountID       uuid.UUID
	CreatedAt       time.Time
	// DEBIT specific
	DailyWithdrawalLimit *float64
	ATMEnabled           *bool
	// VIRTUAL specific
	OnlineOnly           *bool
}

type Transaction struct {
	ID                uuid.UUID
	Type              string // DEPOSIT, WITHDRAWAL, TRANSFER, etc.
	Amount            float64
	Currency          string
	FromAccountID     *uuid.UUID
	ToAccountID       *uuid.UUID
	Status            string
	Description       string
	CreatedAt         time.Time
}

type LedgerEntry struct {
	ID            uuid.UUID
	TransactionID uuid.UUID
	AccountID     uuid.UUID
	Type          string // DEBIT, CREDIT
	Amount        float64
	Currency      string
	Balance       float64
	CreatedAt     time.Time
}

// Türkçe isimler
var turkishFirstNames = []string{
	"Ahmet", "Mehmet", "Mustafa", "Ali", "Hasan", "Hüseyin", "İbrahim", "Osman", "Süleyman", "Yusuf",
	"Ayşe", "Fatma", "Emine", "Hatice", "Zeynep", "Elif", "Meryem", "Sultan", "Hanife", "Şerife",
	"Burak", "Can", "Cem", "Deniz", "Emre", "Eren", "Efe", "Barış", "Arda", "Kerem",
	"Zeynep", "Esra", "Selin", "Dilan", "Merve", "Ceyda", "Aslı", "Derya", "Gizem", "Pınar",
	"Ömer", "Selim", "Furkan", "Berat", "Mert", "Onur", "Serkan", "Tamer", "Volkan", "Yasin",
	"Aylin", "Burcu", "Cansu", "Dilek", "Ebru", "Filiz", "Gonca", "Hande", "İpek", "Jale",
}

var turkishLastNames = []string{
	"Yılmaz", "Kaya", "Demir", "Çelik", "Şahin", "Yıldız", "Yıldırım", "Öztürk", "Aydın", "Özdemir",
	"Arslan", "Doğan", "Aslan", "Çetin", "Kara", "Koç", "Kurt", "Özkan", "Şimşek", "Erdoğan",
	"Aksoy", "Acar", "Polat", "Güneş", "Kaplan", "Korkmaz", "Bulut", "Türk", "Taş", "Karaca",
	"Güler", "Bozkurt", "Özer", "Eren", "Keskin", "Tunç", "Çakır", "Başaran", "Tosun", "Tekin",
	"Durmaz", "Kılıç", "Yaman", "Akın", "Akbulut", "Elmas", "Demirci", "Ateş", "Eryılmaz", "Güven",
}

var turkishCities = []string{
	"İstanbul", "Ankara", "İzmir", "Bursa", "Antalya", "Adana", "Konya", "Gaziantep", "Mersin", "Kayseri",
	"Eskişehir", "Diyarbakır", "Samsun", "Denizli", "Şanlıurfa", "Adapazarı", "Malatya", "Kahramanmaraş", "Erzurum", "Van",
}

var turkishDistricts = []string{
	"Merkez", "Çankaya", "Kadıköy", "Karşıyaka", "Nilüfer", "Muratpaşa", "Seyhan", "Selçuklu", "Yenişehir", "Kocasinan",
	"Odunpazarı", "Bağlar", "İlkadım", "Pamukkale", "Eyyübiye", "Adapazarı", "Battalgazi", "Dulkadiroğlu", "Yakutiye", "İpekyolu",
}

// TC Kimlik numarası üretimi (geçerli algoritma)
func generateTCNo(index int) string {
	// İlk 9 rakam rastgele (ilk rakam 0 olamaz)
	first := rand.Intn(9) + 1
	digits := []int{first}
	for i := 0; i < 8; i++ {
		digits = append(digits, rand.Intn(10))
	}

	// 10. rakam: İlk 9 rakamın toplamının birler basamağı
	sum10 := 0
	for i := 0; i < 9; i++ {
		sum10 += digits[i]
	}
	digit10 := sum10 % 10
	digits = append(digits, digit10)

	// 11. rakam: Tüm rakamların toplamının birler basamağı
	sum11 := sum10 + digit10
	digit11 := sum11 % 10
	digits = append(digits, digit11)

	// String'e çevir
	tc := ""
	for _, d := range digits {
		tc += fmt.Sprintf("%d", d)
	}

	return tc
}

// IBAN oluşturma
func generateIBAN(index int) string {
	// TR + 2 haneli check digit + 5 haneli banka kodu + 1 haneli sıfır + 16 haneli hesap numarası
	// Basitleştirilmiş: TR + checksum + unique number
	// Gerçek IBAN check digit hesabını atladık, UUID kullanıyoruz
	bankCode := "00210" // TrioBank
	accountNumber := fmt.Sprintf("%017d", index+1000000)
	
	// Basit checksum (gerçek IBAN algoritması değil, sadece görsel)
	checksum := (index % 89) + 10 // 10-98 arası
	
	return fmt.Sprintf("TR%02d%s1%s", checksum, bankCode, accountNumber)
}

// Kart numarası oluşturma (Luhn algoritması ile geçerli)
func generateCardNumber(cardType string) string {
	// VISA: 4 ile başlar
	prefix := "4111"
	
	// 12 rastgele rakam
	middle := ""
	for i := 0; i < 12; i++ {
		middle += fmt.Sprintf("%d", rand.Intn(10))
	}
	
	// Luhn check digit hesapla
	cardBase := prefix + middle
	checkDigit := calculateLuhnCheckDigit(cardBase)
	
	return cardBase + fmt.Sprintf("%d", checkDigit)
}

// Luhn algoritması check digit hesaplama
func calculateLuhnCheckDigit(number string) int {
	sum := 0
	parity := len(number) % 2
	
	for i := 0; i < len(number); i++ {
		digit := int(number[i] - '0')
		
		if i%2 == parity {
			digit *= 2
			if digit > 9 {
				digit -= 9
			}
		}
		sum += digit
	}
	
	return (10 - (sum % 10)) % 10
}

// Kart numarasını maskele
func maskCardNumber(cardNumber string) string {
	if len(cardNumber)!= 16 {
		return cardNumber
	}
	return cardNumber[:4] + "********" + cardNumber[12:]
}

// Batch kullanıcı oluşturma
func generateUserBatch(size, startIndex int) []UserData {
	batch := make([]UserData, size)
	
	for i := 0; i < size; i++ {
		userIndex := startIndex + i
		batch[i] = generateUser(userIndex)
	}
	
	return batch
}

// Tek kullanıcı oluşturma
func generateUser(index int) UserData {
	userUUID := uuid.New().String()
	tcNo := generateTCNo(index)
	firstName := turkishFirstNames[rand.Intn(len(turkishFirstNames))]
	lastName := turkishLastNames[rand.Intn(len(turkishLastNames))]
	email := fmt.Sprintf("user_%d_%s@triobank.test", index, userUUID[:8])
	phone := fmt.Sprintf("+90 5%d%d %d%d%d %d%d %d%d",
		rand.Intn(10), rand.Intn(10),
		rand.Intn(10), rand.Intn(10), rand.Intn(10),
		rand.Intn(10), rand.Intn(10),
		rand.Intn(10), rand.Intn(10))
	
	// Doğum tarihi: 18-70 yaş arası
	yearsAgo := 18 + rand.Intn(52)
	birthDate := time.Now().AddDate(-yearsAgo, -rand.Intn(12), -rand.Intn(28))
	
	// Şifre (hepsi aynı: "Triobank123!")
	password := "Triobank123!"
	hashedPwd, _ := hashPassword(password)
	
	// Address
	address := Address{
		Street:     fmt.Sprintf("%s Cad. No: %d", turkishLastNames[rand.Intn(len(turkishLastNames))], rand.Intn(200)+1),
		City:       turkishCities[rand.Intn(len(turkishCities))],
		District:   turkishDistricts[rand.Intn(len(turkishDistricts))],
		PostalCode: fmt.Sprintf("%05d", rand.Intn(90000)+10000),
	}
	
	user := UserData{
		UserUUID:       userUUID,
		TCNo:           tcNo,
		FirstName:      firstName,
		LastName:       lastName,
		Email:          email,
		Phone:          phone,
		BirthDate:      birthDate,
		Password:       password,
		MongoID:        primitive.NewObjectID(),
		HashedPassword: hashedPwd,
		CreatedAt:      time.Now(),
		ClientID:       uuid.New(),
		Address:        address,
	}
	
	// Accounts oluştur (1-3 hesap)
	numAccounts := rand.Intn(3) + 1
	user.Accounts = generateAccounts(user.UserUUID, index, numAccounts)
	
	// Cards oluştur (0-2 kart)
	numCards := rand.Intn(3)
	if numCards > 0 && len(user.Accounts) > 0 {
		user.Cards = generateCards(user, numCards)
	}
	
	// Transactions oluştur (0-10 işlem)
	numTransactions := rand.Intn(11)
	if numTransactions > 0 && len(user.Accounts) > 0 {
		user.Transactions = generateTransactions(user, numTransactions)
		user.LedgerEntries = generateLedgerEntries(user)
	}
	
	return user
}

// Hesaplar oluşturma
func generateAccounts(customerID string, userIndex, count int) []Account {
	accounts := make([]Account, count)
	productCodes := []string{"RETAIL_TRY", "RETAIL_USD", "RETAIL_EUR", "SAVINGS_TRY"}
	currencies := []string{"TRY", "USD", "EUR", "TRY"}
	
	for i := 0; i < count; i++ {
		productIdx := i % len(productCodes)
		
		accounts[i] = Account{
			ID:            uuid.New(),
			CustomerID:    customerID,
			AccountNumber: generateIBAN(userIndex*10 + i),
			ProductCode:   productCodes[productIdx],
			Status:        "ACTIVE",
			Currency:      currencies[productIdx],
			Balance:       float64(rand.Intn(100000) + 1000), // 1K-100K TRY
			CreatedAt:     time.Now().Add(-time.Duration(rand.Intn(365)) * 24 * time.Hour),
			UpdatedAt:     time.Now(),
		}
	}
	
	return accounts
}

// Kartlar oluşturma
func generateCards(user UserData, count int) []Card {
	cards := make([]Card, count)
	
	for i := 0; i < count; i++ {
		accountIdx := rand.Intn(len(user.Accounts))
		accountID := user.Accounts[accountIdx].ID
		
		cardType := "DEBIT"
		if i%2 == 1 {
			cardType = "VIRTUAL"
		}
		
		cardNumber := generateCardNumber(cardType)
		cardholderName := fmt.Sprintf("%s %s", user.FirstName, user.LastName)
		cvv := fmt.Sprintf("%03d", rand.Intn(900)+100)
		
		expiryMonth := rand.Intn(12) + 1
		expiryYear := time.Now().Year() + rand.Intn(3) + 1
		
		card := Card{
			ID:             uuid.New(),
			CardType:       cardType,
			CardNumber:     cardNumber,
			MaskedNumber:   maskCardNumber(cardNumber),
			CVV:            cvv,
			CardholderName: cardholderName,
			ExpiryMonth:    expiryMonth,
			ExpiryYear:     expiryYear,
			CardBrand:      "VISA",
			Status:         "ACTIVE",
			AccountID:      accountID,
			CreatedAt:      time.Now(),
		}
		
		if cardType == "DEBIT" {
			limit := float64(5000)
			enabled := true
			card.DailyWithdrawalLimit = &limit
			card.ATMEnabled = &enabled
		} else {
			online := true
			card.OnlineOnly = &online
		}
		
		cards[i] = card
	}
	
	return cards
}

// İşlemler oluşturma
func generateTransactions(user UserData, count int) []Transaction {
	transactions := make([]Transaction, count)
	txTypes := []string{"DEPOSIT", "WITHDRAWAL", "TRANSFER"}
	
	for i := 0; i < count; i++ {
		accountIdx := rand.Intn(len(user.Accounts))
		account := user.Accounts[accountIdx]
		
		txType := txTypes[rand.Intn(len(txTypes))]
		amount := float64(rand.Intn(10000) + 10)
		
		tx := Transaction{
			ID:          uuid.New(),
			Type:        txType,
			Amount:      amount,
			Currency:    account.Currency,
			Status:      "COMPLETED",
			Description: fmt.Sprintf("%s - %s", txType, account.AccountNumber[:10]),
			CreatedAt:   time.Now().Add(-time.Duration(rand.Intn(90)) * 24 * time.Hour),
		}
		
		if txType == "DEPOSIT" || txType == "WITHDRAWAL" {
			tx.FromAccountID = &account.ID
		} else if txType == "TRANSFER" && len(user.Accounts) > 1 {
			toIdx := (accountIdx + 1) % len(user.Accounts)
			tx.FromAccountID = &account.ID
			tx.ToAccountID = &user.Accounts[toIdx].ID
		}
		
		transactions[i] = tx
	}
	
	return transactions
}

// Ledger entries oluşturma
func generateLedgerEntries(user UserData) []LedgerEntry {
	var entries []LedgerEntry
	
	// Her transaction için 1-2 ledger entry
	for _, tx := range user.Transactions {
		if tx.FromAccountID != nil {
			// Find account
			var account Account
			for _, acc := range user.Accounts {
				if acc.ID == *tx.FromAccountID {
					account = acc
					break
				}
			}
			
			entry := LedgerEntry{
				ID:            uuid.New(),
				TransactionID: tx.ID,
				AccountID:     *tx.FromAccountID,
				Type:          "DEBIT",
				Amount:        tx.Amount,
				Currency:      tx.Currency,
				Balance:       account.Balance - tx.Amount,
				CreatedAt:     tx.CreatedAt,
			}
			entries = append(entries, entry)
		}
		
		if tx.ToAccountID != nil {
			var account Account
			for _, acc := range user.Accounts {
				if acc.ID == *tx.ToAccountID {
					account = acc
					break
				}
			}
			
			entry := LedgerEntry{
				ID:            uuid.New(),
				TransactionID: tx.ID,
				AccountID:     *tx.ToAccountID,
				Type:          "CREDIT",
				Amount:        tx.Amount,
				Currency:      tx.Currency,
				Balance:       account.Balance + tx.Amount,
				CreatedAt:     tx.CreatedAt,
			}
			entries = append(entries, entry)
		}
	}
	
	return entries
}
