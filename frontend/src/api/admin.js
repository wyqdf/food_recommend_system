import request from "@/utils/request";

export const adminApi = {
  login(data) {
    return request.post("/admin/login", data);
  },

  getProfile() {
    return request.get("/admin/profile");
  },

  updatePassword(data) {
    return request.put("/admin/password", data);
  },

  logout() {
    return request.post("/admin/logout");
  },
};

export const adminUserApi = {
  getList(params) {
    return request.get("/admin/users", { params });
  },

  getById(id) {
    return request.get(`/admin/users/${id}`);
  },

  create(data) {
    return request.post("/admin/users", data);
  },

  update(id, data) {
    return request.put(`/admin/users/${id}`, data);
  },

  delete(id) {
    return request.delete(`/admin/users/${id}`);
  },

  batchDelete(ids) {
    return request.delete("/admin/users/batch", { data: { ids } });
  },

  updateStatus(id, data) {
    return request.put(`/admin/users/${id}/status`, data);
  },

  updatePassword(id, data) {
    return request.put(`/admin/users/${id}/password`, data);
  },
};

export const adminRecipeApi = {
  getList(params) {
    return request.get("/admin/recipes", { params });
  },

  getById(id) {
    return request.get(`/admin/recipes/${id}`);
  },

  search(params) {
    return request.get("/recipes/search", { params });
  },

  create(data) {
    return request.post("/admin/recipes", data);
  },

  update(id, data) {
    return request.put(`/admin/recipes/${id}`, data);
  },

  delete(id) {
    return request.delete(`/admin/recipes/${id}`);
  },

  batchDelete(ids) {
    return request.delete("/admin/recipes/batch", { data: { ids } });
  },

  audit(id, data) {
    return request.put(`/admin/recipes/${id}/audit`, data);
  },
};

export const adminCategoryApi = {
  getList() {
    return request.get("/admin/categories");
  },

  create(data) {
    return request.post("/admin/categories", data);
  },

  update(id, data) {
    return request.put(`/admin/categories/${id}`, data);
  },

  delete(id) {
    return request.delete(`/admin/categories/${id}`);
  },
};

export const adminStatisticsApi = {
  getOverview() {
    return request.get("/admin/statistics/overview");
  },

  getUsers(params) {
    return request.get("/admin/statistics/users", { params });
  },

  getRecipes(params) {
    return request.get("/admin/statistics/recipes", { params });
  },

  getComments(params) {
    return request.get("/admin/statistics/comments", { params });
  },

  getAdvanced() {
    return request.get("/admin/statistics/advanced");
  },

  getDifficultyDistribution() {
    return request.get("/admin/statistics/difficulty-distribution");
  },

  getTimeCostDistribution() {
    return request.get("/admin/statistics/timecost-distribution");
  },

  getMonthlyTrend() {
    return request.get("/admin/statistics/monthly-trend");
  },

  refreshStatistics() {
    return request.post("/admin/statistics/refresh", null, {
      timeout: 45000,
    });
  },
};

export const adminLogApi = {
  getList(params) {
    return request.get("/admin/logs", { params });
  },

  getById(id) {
    return request.get(`/admin/logs/${id}`);
  },

  delete(id) {
    return request.delete(`/admin/logs/${id}`);
  },

  batchDelete(ids) {
    return request.delete("/admin/logs/batch", { data: { ids } });
  },

  cleanup(beforeDays) {
    return request.delete("/admin/logs/cleanup", { params: { beforeDays } });
  },

  getMeta() {
    return request.get("/admin/logs/meta");
  },
};

export const adminTasteApi = {
  getList() {
    return request.get("/admin/tastes");
  },

  create(data) {
    return request.post("/admin/tastes", data);
  },

  update(id, data) {
    return request.put(`/admin/tastes/${id}`, data);
  },

  delete(id) {
    return request.delete(`/admin/tastes/${id}`);
  },
};

export const adminTechniqueApi = {
  getList() {
    return request.get("/admin/techniques");
  },

  create(data) {
    return request.post("/admin/techniques", data);
  },

  update(id, data) {
    return request.put(`/admin/techniques/${id}`, data);
  },

  delete(id) {
    return request.delete(`/admin/techniques/${id}`);
  },
};

export const adminTimeCostApi = {
  getList() {
    return request.get("/admin/time-costs");
  },

  create(data) {
    return request.post("/admin/time-costs", data);
  },

  update(id, data) {
    return request.put(`/admin/time-costs/${id}`, data);
  },

  delete(id) {
    return request.delete(`/admin/time-costs/${id}`);
  },
};

export const adminDifficultyApi = {
  getList() {
    return request.get("/admin/difficulties");
  },

  create(data) {
    return request.post("/admin/difficulties", data);
  },

  update(id, data) {
    return request.put(`/admin/difficulties/${id}`, data);
  },

  delete(id) {
    return request.delete(`/admin/difficulties/${id}`);
  },
};

export const adminIngredientApi = {
  getList() {
    return request.get("/admin/ingredients");
  },

  create(data) {
    return request.post("/admin/ingredients", data);
  },

  update(id, data) {
    return request.put(`/admin/ingredients/${id}`, data);
  },

  delete(id) {
    return request.delete(`/admin/ingredients/${id}`);
  },
};
