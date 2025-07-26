import { createI18n } from 'vue-i18n';
import zh from './locales/zh.js';
import en from './locales/en.js';

const messages = {
  zh,
  en
};

// 从localStorage获取语言设置，默认为中文
const locale = localStorage.getItem('language') || 'zh';

const i18n = createI18n({
  legacy: false,
  locale,
  fallbackLocale: 'zh',
  messages
});

export default i18n;