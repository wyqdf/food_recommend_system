import request from "@/utils/request";

let categoriesCache = null;
let categoriesCacheTime = 0;
let recommendCategoriesCache = null;
let recommendCategoriesCacheTime = 0;

export const recipeApi = {
  getList(params) {
    return request.get("/recipes", { params });
  },

  getDetail(id) {
    return request.get(`/recipes/${id}`);
  },

  search(params, config = {}) {
    return request.get("/recipes/search", { params, ...config });
  },

  getSearchSuggestions(params) {
    return request.get("/recipes/search/suggestions", {
      params,
      timeout: 10000,
      silentError: true,
    });
  },

  async getCategories() {
    const now = Date.now();
    if (categoriesCache && now - categoriesCacheTime < 600000) {
      return { data: categoriesCache };
    }
    const res = await request.get("/categories");
    categoriesCache = res.data;
    categoriesCacheTime = now;
    return res;
  },

  async getRecommendCategories(limit = 10) {
    const now = Date.now();
    if (recommendCategoriesCache && now - recommendCategoriesCacheTime < 600000) {
      return { data: recommendCategoriesCache.slice(0, limit) };
    }
    const res = await request.get("/categories/recommend", { params: { limit } });
    recommendCategoriesCache = Array.isArray(res.data) ? res.data : [];
    recommendCategoriesCacheTime = now;
    return { data: recommendCategoriesCache.slice(0, limit) };
  },

  getRecommend(params) {
    return request.get("/recipes/recommend", { params, timeout: 20000, silentError: true });
  },

  getSimilar(id, params) {
    return request.get(`/recipes/${id}/similar`, { params, timeout: 20000, silentError: true });
  },

  create(data) {
    return request.post("/recipes", data);
  },
};

export const userApi = {
  login(data) {
    return request.post("/users/login", data);
  },

  register(data) {
    return request.post("/users/register", data);
  },

  getProfile() {
    return request.get("/users/profile");
  },

  updateProfile(data) {
    return request.put("/users/profile", data);
  },

  getOnboarding() {
    return request.get("/users/onboarding");
  },

  updateOnboarding(data) {
    return request.put("/users/onboarding", data);
  },

  get7dReport() {
    return request.get("/users/reports/7d");
  },
};

export const favoriteApi = {
  getList(params) {
    return request.get("/favorites", { params });
  },

  add(recipeId) {
    return request.post("/favorites", { recipeId });
  },

  remove(recipeId) {
    return request.delete(`/favorites/${recipeId}`);
  },

  check(recipeId) {
    return request.get(`/favorites/check/${recipeId}`);
  },
};

export const commentApi = {
  getList(recipeId, params) {
    return request.get(`/comments/recipe/${recipeId}`, { params });
  },

  add(data) {
    return request.post("/comments", data);
  },

  like(commentId) {
    return request.post(`/comments/${commentId}/like`);
  },
};

export const attributeApi = {
  getTastes() {
    return request.get("/tastes");
  },

  getTechniques() {
    return request.get("/techniques");
  },

  getTimeCosts() {
    return request.get("/time-costs");
  },

  getDifficulties() {
    return request.get("/difficulties");
  },

  getIngredients() {
    return request.get("/ingredients");
  },

  getCookwares() {
    return request.get("/cookwares");
  },
};

export const sceneApi = {
  getList() {
    return request.get("/scenes");
  },
};

export const analyticsApi = {
  batchEvents(data) {
    return request.post("/analytics/events/batch", data);
  },
};

export const cookingApi = {
  startSession(recipeId) {
    return request.post("/users/cooking-sessions/start", { recipeId });
  },

  updateProgress(sessionId, data) {
    return request.put(`/users/cooking-sessions/${sessionId}/progress`, data);
  },

  finishSession(sessionId, data = {}) {
    return request.post(`/users/cooking-sessions/${sessionId}/finish`, data);
  },
};

export const uploadApi = {
  uploadImage(file) {
    const formData = new FormData();
    formData.append("file", file);
    return request.post("/upload/image", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  },

  uploadRecipeImage(file, recipeId) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("recipeId", recipeId);
    return request.post("/upload/recipe-image", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  },

  deleteImage(url) {
    return request.delete("/upload/image", { data: { url } });
  },
};
