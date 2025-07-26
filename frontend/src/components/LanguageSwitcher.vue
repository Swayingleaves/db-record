<template>
  <div class="language-switcher">
    <select v-model="currentLanguage" @change="changeLanguage" class="language-select">
      <option value="zh">{{ $t('language.chinese') }}</option>
      <option value="en">{{ $t('language.english') }}</option>
    </select>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

export default defineComponent({
  name: 'LanguageSwitcher',
  setup() {
    const { locale } = useI18n();
    const currentLanguage = ref(locale.value);

    onMounted(() => {
      currentLanguage.value = locale.value;
    });

    function changeLanguage() {
      locale.value = currentLanguage.value;
      localStorage.setItem('language', currentLanguage.value);
    }

    return {
      currentLanguage,
      changeLanguage
    };
  }
});
</script>

<style scoped>
.language-switcher {
  display: inline-block;
}

.language-select {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.language-select:hover {
  background: rgba(255, 255, 255, 0.2);
}

.language-select option {
  background: #fff;
  color: #333;
}
</style>