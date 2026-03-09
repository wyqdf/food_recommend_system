import request from "@/utils/request";

let categoriesCache = null;
let categoriesCacheTime = 0;

export const recipeApi = {
  getList(params) {
    return request.get("/recipes", { params });
  },

  getDetail(id) {
    return request.get(`/recipes/${id}`);
  },

  search(params) {
    return request.get("/recipes/search", { params });
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

  getRecommend(params) {
    return request.get("/recipes/recommend", { params });
  },

  getSimilar(id, params) {
    return request.get(`/recipes/${id}/similar`, { params });
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
