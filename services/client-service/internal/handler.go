package internal

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/google/uuid"
)

type Handler struct {
	Repo Repo
}

func NewHandler(repo Repo) *Handler {
	return &Handler{Repo: repo}
}

// ClientsHandler handles /clients (POST and GET)
func (h *Handler) ClientsHandler(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodPost:
		h.CreateClient(w, r)
	case http.MethodGet:
		h.ListClients(w, r)
	default:
		respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
	}
}

// ClientByIDHandler handles /clients/{id} (GET, PUT, DELETE)
func (h *Handler) ClientByIDHandler(w http.ResponseWriter, r *http.Request) {
	// Extract ID from path
	idStr := extractIDFromPath(r.URL.Path)
	if idStr == "" {
		respondError(w, http.StatusBadRequest, "Client ID is required")
		return
	}

	id, err := uuid.Parse(idStr)
	if err != nil {
		respondError(w, http.StatusBadRequest, "Invalid client ID format")
		return
	}

	switch r.Method {
	case http.MethodGet:
		h.GetClientByID(w, r, id)
	case http.MethodPut:
		h.UpdateClient(w, r, id)
	case http.MethodDelete:
		h.DeleteClient(w, r, id)
	default:
		respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
	}
}

// CreateClient - POST /clients
func (h *Handler) CreateClient(w http.ResponseWriter, r *http.Request) {
	var req CreateClientRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	client := &Client{
		ID:        uuid.New(),
		UserID:    req.UserID,
		TCNo:      req.TCNo,
		FirstName: req.FirstName,
		LastName:  req.LastName,
		Email:     req.Email,
		GSM:       req.GSM,
		BirthDate: req.BirthDate,
		Address:   req.Address,
		CreatedAt: time.Now().UTC(),
	}

	if err := h.Repo.Db.CreateClient(r.Context(), client); err != nil {
		log.Printf("CreateClient error: %v", err)
		respondError(w, http.StatusInternalServerError, "Failed to create client")
		return
	}

	respondJSON(w, http.StatusCreated, client)
}

// GetClientByID - GET /clients/{id}
func (h *Handler) GetClientByID(w http.ResponseWriter, r *http.Request, id uuid.UUID) {
	client, err := h.Repo.Db.GetClientByID(r.Context(), id)
	if err != nil {
		if err == ErrClientNotFound {
			respondError(w, http.StatusNotFound, "Client not found")
			return
		}
		log.Printf("GetClientByID error: %v", err)
		respondError(w, http.StatusInternalServerError, "Failed to get client")
		return
	}

	respondJSON(w, http.StatusOK, client)
}

// ListClients - GET /clients
func (h *Handler) ListClients(w http.ResponseWriter, r *http.Request) {
	page, _ := strconv.Atoi(r.URL.Query().Get("page"))
	limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))

	if page < 1 {
		page = 1
	}
	if limit < 1 || limit > 100 {
		limit = 20
	}

	req := PaginationRequest{Page: page, Limit: limit}
	clients, total, err := h.Repo.Db.ListClients(r.Context(), req)
	if err != nil {
		log.Printf("ListClients error: %v", err)
		respondError(w, http.StatusInternalServerError, "Failed to list clients")
		return
	}

	respondJSON(w, http.StatusOK, map[string]interface{}{
		"data":  clients,
		"total": total,
		"page":  page,
		"limit": limit,
	})
}

// UpdateClient - PUT /clients/{id}
func (h *Handler) UpdateClient(w http.ResponseWriter, r *http.Request, id uuid.UUID) {
	var req UpdateClient
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Prevent email from being updated if it's empty string or nil
	// Email should only be updated explicitly, not accidentally cleared
	if req.Email != nil && *req.Email == "" {
		req.Email = nil
	}

	client, err := h.Repo.Db.UpdateClient(r.Context(), id, req)
	if err != nil {
		if err == ErrClientNotFound {
			respondError(w, http.StatusNotFound, "Client not found")
			return
		}
		log.Printf("UpdateClient error: %v", err)
		respondJSON(w, http.StatusInternalServerError, map[string]string{"error": "Failed to update client"})
		return
	}

	respondJSON(w, http.StatusOK, client)
}

// DeleteClient - DELETE /clients/{id}
func (h *Handler) DeleteClient(w http.ResponseWriter, r *http.Request, id uuid.UUID) {
	if err := h.Repo.Db.DeleteClient(r.Context(), id); err != nil {
		if err == ErrClientNotFound {
			respondError(w, http.StatusNotFound, "Client not found")
			return
		}
		log.Printf("DeleteClient error: %v", err)
		respondError(w, http.StatusInternalServerError, "Failed to delete client")
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// Helper functions
func respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func respondError(w http.ResponseWriter, status int, message string) {
	respondJSON(w, status, map[string]string{"error": message})
}

func extractIDFromPath(path string) string {
	parts := strings.Split(strings.Trim(path, "/"), "/")
	if len(parts) >= 2 {
		return parts[1]
	}
	return ""
}

// ClientByUserIDHandler handles /clients/user/{userID} (GET)
func (h *Handler) ClientByUserIDHandler(w http.ResponseWriter, r *http.Request) {
	// Extract UserID from path: /clients/user/{userID}
	pathParts := strings.Split(strings.Trim(r.URL.Path, "/"), "/")
	if len(pathParts) < 3 || pathParts[2] == "" {
		respondError(w, http.StatusBadRequest, "User ID is required")
		return
	}
	idStr := pathParts[2]

	userID, err := uuid.Parse(idStr)
	if err != nil {
		respondError(w, http.StatusBadRequest, "Invalid user ID format")
		return
	}

	switch r.Method {
	case http.MethodGet:
		h.GetClientByUserID(w, r, userID)
	default:
		respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
	}
}

// GetClientByUserID - GET /clients/user/{userID}
func (h *Handler) GetClientByUserID(w http.ResponseWriter, r *http.Request, userID uuid.UUID) {
	client, err := h.Repo.Db.GetClientByUserID(r.Context(), userID)
	if err != nil {
		if err == ErrClientNotFound {
			respondError(w, http.StatusNotFound, "Client not found for this user")
			return
		}
		log.Printf("GetClientByUserID error: %v", err)
		respondError(w, http.StatusInternalServerError, "Failed to get client")
		return
	}

	respondJSON(w, http.StatusOK, client)
}

// Request types
type CreateClientRequest struct {
	UserID    uuid.UUID `json:"user_id"`
	TCNo      string    `json:"tc_no"`
	FirstName string    `json:"first_name"`
	LastName  string    `json:"last_name"`
	Email     string    `json:"email"`
	GSM       string    `json:"gsm"`
	BirthDate time.Time `json:"birth_date"`
	Address   Address   `json:"address"`
}
