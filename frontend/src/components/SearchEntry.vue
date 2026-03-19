<template>
  <div class="search-entry">
    <el-autocomplete
      v-model="innerValue"
      :fetch-suggestions="fetchSuggestions"
      :trigger-on-focus="showHistoryOnEmpty"
      :debounce="180"
      :clearable="clearable"
      :placeholder="placeholder"
      :size="size"
      class="search-entry__input"
      @select="handleSelect"
      @keyup.enter="handleSubmit"
    >
      <template #prefix>
        <el-icon>
          <Search />
        </el-icon>
      </template>
      <template #default="{ item }">
        <div class="suggestion-item">
          <span class="suggestion-item__value">{{ item.value }}</span>
          <span class="suggestion-item__type">{{ item.typeLabel }}</span>
        </div>
      </template>
    </el-autocomplete>
    <el-button
      v-if="showButton"
      :type="buttonType"
      :size="size"
      class="search-entry__button"
      @click="handleSubmit"
    >
      {{ buttonText }}
    </el-button>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { Search } from "@element-plus/icons-vue";
import { recipeApi } from "@/api";
import { addSearchHistory, getSearchHistory } from "@/utils/search";
import { trackBehavior } from "@/utils/tracker";

const props = defineProps({
  modelValue: { type: String, default: "" },
  placeholder: { type: String, default: "搜索菜名、食材、作者..." },
  sourcePage: { type: String, default: "search_page" },
  size: { type: String, default: "default" },
  suggestionLimit: { type: Number, default: 8 },
  showHistoryOnEmpty: { type: Boolean, default: true },
  showButton: { type: Boolean, default: true },
  buttonText: { type: String, default: "搜索" },
  buttonType: { type: String, default: "primary" },
  clearable: { type: Boolean, default: true },
});

const emit = defineEmits(["update:modelValue", "submit"]);

const requestId = ref(0);

const innerValue = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const normalizeKeyword = (keyword) => {
  if (typeof keyword !== "string") return "";
  return keyword.trim().replace(/\s+/g, " ");
};

const buildHistoryItems = () =>
  getSearchHistory().map((value) => ({
    value,
    type: "history",
    typeLabel: "最近搜索",
  }));

const fetchSuggestions = async (queryString, cb) => {
  const currentRequestId = ++requestId.value;
  const normalizedKeyword = normalizeKeyword(queryString);

  if (!normalizedKeyword) {
    cb(props.showHistoryOnEmpty ? buildHistoryItems() : []);
    return;
  }

  try {
    const res = await recipeApi.getSearchSuggestions({
      keyword: normalizedKeyword,
      limit: props.suggestionLimit,
    });
    if (currentRequestId !== requestId.value) return;

    const list = Array.isArray(res.data)
      ? res.data
          .map((item) => ({
            value: normalizeKeyword(item.value),
            type: item.type || "title",
            typeLabel: item.typeLabel || "建议词",
          }))
          .filter((item) => item.value)
      : [];
    cb(list);
  } catch (error) {
    if (currentRequestId === requestId.value) {
      cb([]);
    }
  }
};

const submitSearch = (rawKeyword, eventType, sourcePage, extra = {}) => {
  const normalizedKeyword = normalizeKeyword(rawKeyword);
  if (!normalizedKeyword) return;

  emit("update:modelValue", normalizedKeyword);
  addSearchHistory(normalizedKeyword);
  trackBehavior(eventType, {
    sourcePage,
    extra: { keyword: normalizedKeyword, ...extra },
  });
  emit("submit", {
    keyword: normalizedKeyword,
    eventType,
    sourcePage,
    extra,
  });
};

const handleSubmit = () => {
  submitSearch(innerValue.value, "search_submit", props.sourcePage);
};

const handleSelect = (item) => {
  if (!item?.value) return;
  const isHistory = item.type === "history";
  submitSearch(
    item.value,
    isHistory ? "search_history_click" : "search_suggestion_click",
    isHistory ? `${props.sourcePage}_history` : `${props.sourcePage}_suggestion`,
    isHistory ? {} : { suggestionType: item.type }
  );
};
</script>

<style scoped>
.search-entry {
  display: flex;
  align-items: stretch;
  gap: 10px;
  width: 100%;
}

.search-entry__input {
  flex: 1;
}

.search-entry__button {
  flex-shrink: 0;
  min-width: 88px;
  font-weight: 600;
}

.suggestion-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
}

.suggestion-item__value {
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggestion-item__type {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 640px) {
  .search-entry__button {
    min-width: 74px;
  }
}
</style>
