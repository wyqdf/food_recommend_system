const SEARCH_HISTORY_KEY = "search_history_keywords";
const SEARCH_HISTORY_LIMIT = 8;

export const SEARCH_HOT_KEYWORDS = [
  "红烧肉",
  "可乐鸡翅",
  "糖醋排骨",
  "宫保鸡丁",
  "麻婆豆腐",
  "家常菜",
  "减脂餐",
  "下饭菜",
];

const normalizeKeyword = (keyword) => {
  if (typeof keyword !== "string") return "";
  return keyword.trim().replace(/\s+/g, " ");
};

export const getSearchHistory = () => {
  try {
    const raw = localStorage.getItem(SEARCH_HISTORY_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => normalizeKeyword(item))
      .filter(Boolean)
      .slice(0, SEARCH_HISTORY_LIMIT);
  } catch (error) {
    return [];
  }
};

export const addSearchHistory = (keyword) => {
  const normalized = normalizeKeyword(keyword);
  if (!normalized) return getSearchHistory();

  const next = [normalized, ...getSearchHistory().filter((item) => item !== normalized)].slice(0, SEARCH_HISTORY_LIMIT);
  localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next));
  return next;
};

export const removeSearchHistory = (keyword) => {
  const normalized = normalizeKeyword(keyword);
  const next = getSearchHistory().filter((item) => item !== normalized);
  localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next));
  return next;
};

export const clearSearchHistory = () => {
  localStorage.removeItem(SEARCH_HISTORY_KEY);
  return [];
};
