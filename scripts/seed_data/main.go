package main

import (
"context"
"database/sql"
"flag"
"fmt"
"log"
"math/rand"
"os"
"time"

"github.com/google/uuid"
"github.com/joho/godotenv"
_ "github.com/microsoft/go-mssqldb"
)

// Config
type Config struct {
ClientDBConn  string
AccountDBConn string
TotalClients  int
BatchSize     int
}

// Client model
type Client struct {
ID         string
UserID     string
TCNo       string
FirstName  string
LastName   string
Email      string
GSM        string
BirthDate  time.Time
Street     string
City       string
District   string
PostalCode string
Status     string
CreatedAt  time.Time
}

// Türkçe veriler
var firstNames = []string{
"Ahmet", "Mehmet", "Mustafa", "Ali", "Hasan", "Hüseyin", "İbrahim", "Osman", "Süleyman", "Yusuf",
"Ayşe", "Fatma", "Emine", "Hatice", "Zeynep", "Elif", "Meryem", "Sultan", "Hanife", "Şerife",
"Burak", "Can", "Cem", "Deniz", "Emre", "Eren", "Efe", "Barış", "Arda", "Kerem",
"Selin", "Esra", "Dilan", "Merve", "Ceyda", "Aslı", "Derya", "Gizem", "Pınar", "Aylin",
}

var lastNames = []string{
"Yılmaz", "Kaya", "Demir", "Çelik", "Şahin", "Yıldız", "Yıldırım", "Öztürk", "Aydın", "Özdemir",
"Arslan", "Doğan", "Aslan", "Çetin", "Kara", "Koç", "Kurt", "Özkan", "Şimşek", "Erdoğan",
"Aksoy", "Acar", "Polat", "Güneş", "Kaplan", "Korkmaz", "Bulut", "Türk", "Taş", "Karaca",
}

var cities = []string{
"İstanbul", "Ankara", "İzmir", "Bursa", "Antalya", "Adana", "Konya", "Gaziantep", "Mersin", "Kayseri",
}

var districts = []string{
"Merkez", "Çankaya", "Kadıköy", "Karşıyaka", "Nilüfer", "Muratpaşa", "Seyhan", "Selçuklu",
}

// Global account counter for unique IBANs
var globalAccountCounter = 0

func main() {
verify := flag.Bool("verify", false, "Sadece kayıt sayılarını kontrol et")
flag.Parse()

cfg := loadConfig()

if *verify {
verifyData(cfg)
return
}

log.Println("🚀 TrioBank Mikroservis Seed Started...")
log.Printf("📊 Target: %d clients with accounts\n", cfg.TotalClients)
log.Printf("📦 Batch size: %d\n", cfg.BatchSize)

clientDB := connectMSSQL(cfg.ClientDBConn, "client-service")
accountDB := connectMSSQL(cfg.AccountDBConn, "account-service")

defer clientDB.Close()
defer accountDB.Close()

rand.Seed(time.Now().UnixNano())

startTime := time.Now()
totalProcessed := 0

for i := 0; i < cfg.TotalClients; i += cfg.BatchSize {
batchStart := time.Now()
currentBatchSize := cfg.BatchSize
if i+cfg.BatchSize > cfg.TotalClients {
currentBatchSize = cfg.TotalClients - i
}

log.Printf("\n📦 Processing batch %d-%d (size: %d)...", i+1, i+currentBatchSize, currentBatchSize)

// 1. Client'ları oluştur
clients := generateClients(currentBatchSize, i)

// 2. Client DB'ye ekle
if err := insertClients(clientDB, clients); err != nil {
log.Fatalf("❌ Failed to insert clients: %v", err)
}
log.Printf("✅ %d clients inserted", len(clients))

// 3. Her client için account + outbox oluştur (transaction ile)
if err := insertAccountsWithOutbox(accountDB, clients); err != nil {
log.Fatalf("❌ Failed to insert accounts with outbox: %v", err)
}
log.Printf("✅ %d accounts + outbox events inserted", len(clients))

totalProcessed += currentBatchSize
elapsed := time.Since(batchStart)
totalElapsed := time.Since(startTime)
avgTimePerClient := totalElapsed.Seconds() / float64(totalProcessed)
remainingClients := cfg.TotalClients - totalProcessed
estimatedRemaining := time.Duration(float64(remainingClients)*avgTimePerClient) * time.Second

log.Printf("⏱  Batch completed in %s | Progress: %d/%d (%.1f%%) | Est. remaining: %s",
elapsed.Round(time.Second),
totalProcessed,
cfg.TotalClients,
float64(totalProcessed)/float64(cfg.TotalClients)*100,
estimatedRemaining.Round(time.Second),
)
}

totalDuration := time.Since(startTime)
log.Printf("\n🎉 ✨ Data seeding completed! ✨")
log.Printf("📊 Total clients created: %d", cfg.TotalClients)
log.Printf("⏱  Total time: %s", totalDuration.Round(time.Second))
log.Printf("⚡ Average: %.2f clients/second", float64(cfg.TotalClients)/totalDuration.Seconds())
}

func loadConfig() Config {
_ = godotenv.Load("config.env")

return Config{
ClientDBConn:  getEnv("CLIENT_DB_URI", "sqlserver://sa:TrioBank123@localhost:1433?database=client_db&encrypt=false&trustServerCertificate=true"),
AccountDBConn: getEnv("ACCOUNT_DB_URI", "sqlserver://sa:TrioBank123@localhost:1433?database=account_db&encrypt=false&trustServerCertificate=true"),
TotalClients:  getEnvInt("TOTAL_CLIENTS", 10),
BatchSize:     getEnvInt("BATCH_SIZE", 1000),
}
}

func getEnv(key, defaultValue string) string {
if value := os.Getenv(key); value != "" {
return value
}
return defaultValue
}

func getEnvInt(key string, defaultValue int) int {
if value := os.Getenv(key); value != "" {
var intVal int
fmt.Sscanf(value, "%d", &intVal)
return intVal
}
return defaultValue
}

func connectMSSQL(connString, serviceName string) *sql.DB {
db, err := sql.Open("sqlserver", connString)
if err != nil {
log.Fatalf("MSSQL (%s) connection error: %v", serviceName, err)
}

db.SetMaxOpenConns(50)
db.SetMaxIdleConns(50)
db.SetConnMaxLifetime(5 * time.Minute)

if err := db.Ping(); err != nil {
log.Fatalf("MSSQL (%s) ping error: %v", serviceName, err)
}

log.Printf("✅ MSSQL connected: %s", serviceName)
return db
}

// TC Kimlik numarası üretimi (geçerli algoritma)
func generateTCNo(index int) string {
first := rand.Intn(9) + 1
digits := []int{first}
for i := 0; i < 8; i++ {
digits = append(digits, rand.Intn(10))
}

sum10 := 0
for i := 0; i < 9; i++ {
sum10 += digits[i]
}
digit10 := sum10 % 10
digits = append(digits, digit10)

sum11 := sum10 + digit10
digit11 := sum11 % 10
digits = append(digits, digit11)

tc := ""
for _, d := range digits {
tc += fmt.Sprintf("%d", d)
}
return tc
}

// IBAN oluşturma - Global counter kullanarak unique
func generateIBAN(counter int) string {
bankCode := "00210" // TrioBank
// 17 haneli unique number (10 milyon kadar unique IBAN üretebilir)
accountNumber := fmt.Sprintf("%017d", 1000000+counter)
// Checksum (basitleştirilmiş)
checksum := (counter % 89) + 10 // 10-98 arası
return fmt.Sprintf("TR%02d%s1%s", checksum, bankCode, accountNumber)
}

// Client'ları oluştur
func generateClients(count, startIndex int) []Client {
clients := make([]Client, count)

for i := 0; i < count; i++ {
index := startIndex + i
userID := uuid.New().String()
clientID := uuid.New().String()

yearsAgo := 18 + rand.Intn(52)
birthDate := time.Now().AddDate(-yearsAgo, -rand.Intn(12), -rand.Intn(28))

clients[i] = Client{
ID:         clientID,
UserID:     userID,
TCNo:       generateTCNo(index),
FirstName:  firstNames[rand.Intn(len(firstNames))],
LastName:   lastNames[rand.Intn(len(lastNames))],
Email:      fmt.Sprintf("user_%d_%s@triobank.test", index, clientID[:8]),
GSM: fmt.Sprintf("+90 5%d%d %d%d%d %d%d %d%d",
rand.Intn(10), rand.Intn(10),
rand.Intn(10), rand.Intn(10), rand.Intn(10),
rand.Intn(10), rand.Intn(10),
rand.Intn(10), rand.Intn(10)),
BirthDate:  birthDate,
Street:     fmt.Sprintf("%s Cad. No: %d", lastNames[rand.Intn(len(lastNames))], rand.Intn(200)+1),
City:       cities[rand.Intn(len(cities))],
District:   districts[rand.Intn(len(districts))],
PostalCode: fmt.Sprintf("%05d", rand.Intn(90000)+10000),
Status:     "ACTIVE",
CreatedAt:  time.Now().UTC(),
}
}

return clients
}

// Client'ları DB'ye ekle
func insertClients(db *sql.DB, clients []Client) error {
ctx := context.Background()

for _, client := range clients {
query := `
INSERT INTO clients (
id, user_id, tc_no, first_name, last_name,
email, gsm, birth_date,
address_street, address_city, address_district, address_postal_code,
status, created_at
) VALUES (
@p1, @p2, @p3, @p4, @p5,
@p6, @p7, @p8,
@p9, @p10, @p11, @p12,
@p13, @p14
)`

_, err := db.ExecContext(ctx, query,
client.ID,
client.UserID,
client.TCNo,
client.FirstName,
client.LastName,
client.Email,
client.GSM,
client.BirthDate,
client.Street,
client.City,
client.District,
client.PostalCode,
client.Status,
client.CreatedAt,
)
if err != nil {
return fmt.Errorf("failed to insert client %s: %w", client.ID, err)
}
}

return nil
}

// Account + Outbox eventi birlikte ekle (transaction ile)
// Global counter kullanarak unique IBAN garanti edilir
func insertAccountsWithOutbox(db *sql.DB, clients []Client) error {
ctx := context.Background()

for _, client := range clients {
// Transaction başlat
tx, err := db.BeginTx(ctx, nil)
if err != nil {
return fmt.Errorf("failed to begin transaction: %w", err)
}

// Account oluştur - Global counter ile unique IBAN
accountID := uuid.New().String()
globalAccountCounter++ // Atomic increment
accountNumber := generateIBAN(globalAccountCounter)

// Status: %50 ACTIVE, %50 CLOSED
status := "ACTIVE"
if rand.Float32() < 0.5 {
status = "CLOSED"
}

balance := float64(rand.Intn(100000) + 1000) // 1K-100K TRY
createdAt := time.Now().UTC()

// Configurations JSON
configurations := `{"emailNotifications":true,"dailyTransactionLimit":50000.0,"dailyWithdrawalLimit":10000.0,"smsNotifications":false}`

// 1. Account'u ekle
accountQuery := `
INSERT INTO accounts (
id, customer_id, account_number, product_code,
currency, balance, status, configurations,
version, created_at
) VALUES (
@p1, @p2, @p3, @p4,
@p5, @p6, @p7, @p8,
@p9, @p10
)`

_, err = tx.ExecContext(ctx, accountQuery,
accountID,
client.UserID, // customer_id = user_id
accountNumber,
"CHECKING_TRY",
"TRY",
balance,
status,
configurations,
0, // version
createdAt,
)
if err != nil {
tx.Rollback()
return fmt.Errorf("failed to insert account %s: %w", accountID, err)
}

// 2. Outbox event ekle (AccountCreated)
outboxID := uuid.New()
eventType := "AccountCreated"
aggregateType := "Account"

// Payload JSON
payload := fmt.Sprintf(`{"currency":"TRY","createdBy":"SYSTEM","createdAt":"%s","customerId":"%s","accountId":"%s","accountType":"CHECKING","accountNumber":"%s","status":"%s"}`,
createdAt.Format(time.RFC3339Nano),
client.UserID,
accountID,
accountNumber,
status,
)

outboxQuery := `
INSERT INTO outbox_events (
id, aggregate_type, aggregate_id, type, payload, created_at
) VALUES (
@p1, @p2, @p3, @p4, @p5, @p6
)`

_, err = tx.ExecContext(ctx, outboxQuery,
outboxID,
aggregateType,
accountID,
eventType,
payload,
createdAt,
)
if err != nil {
tx.Rollback()
return fmt.Errorf("failed to insert outbox event for account %s: %w", accountID, err)
}

// Transaction commit
if err := tx.Commit(); err != nil {
return fmt.Errorf("failed to commit transaction: %w", err)
}
}

return nil
}

// Verify fonksiyonu
func verifyData(cfg Config) {
log.Println("🔍 Verifying data counts...")

clientDB := connectMSSQL(cfg.ClientDBConn, "client-service")
defer clientDB.Close()

var clientCount int
clientDB.QueryRow("SELECT COUNT(*) FROM clients").Scan(&clientCount)
log.Printf("📊 Clients: %d", clientCount)

accountDB := connectMSSQL(cfg.AccountDBConn, "account-service")
defer accountDB.Close()

var accountCount int
accountDB.QueryRow("SELECT COUNT(*) FROM accounts").Scan(&accountCount)
log.Printf("📊 Accounts: %d", accountCount)

var outboxCount int
accountDB.QueryRow("SELECT COUNT(*) FROM outbox_events WHERE type = 'AccountCreated'").Scan(&outboxCount)
log.Printf("📊 Outbox Events (AccountCreated): %d", outboxCount)

log.Println("✅ Verification completed")
}
