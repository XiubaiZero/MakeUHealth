<template>
  <div class="card">
    <h2 style="margin-bottom: 20px; color: var(--text-primary);"> {{ t("Personal Information") }} </h2>

    <div class="form-group" style="margin-bottom: 20px;">
      <label style="display: block; margin-bottom: 8px; color: var(--text-secondary);"> {{ t("Age") }} </label>
      <input
          v-model="user.age"
          type="number"
          class="input-field"
          :placeholder="t('Enter age')"
      >
    </div>

    <div class="form-group" style="margin-bottom: 20px;">
      <label style="display: block; margin-bottom: 8px; color: var(--text-secondary);"> {{ t("Gender") }} </label>
      <div class="radio-group">
        <div
            :class="['radio-option', user.gender === 'male' ? 'active' : '']"
            @click="user.gender = 'male'"
        > {{ t("male") }} </div>
        <div
            :class="['radio-option', user.gender === 'female' ? 'active' : '']"
            @click="user.gender = 'female'"
        > {{ t("female") }} </div>
      </div>
    </div>

    <div style="display: flex; gap: 20px; margin-bottom: 20px;">
      <div style="flex: 1;">
        <label style="display: block; margin-bottom: 8px; color: var(--text-secondary);"> {{ t("Height (cm)") }} </label>
        <input
            v-model="user.height"
            type="number"
            step="0.1"
            class="input-field"
            :placeholder="t('Height in cm')"
        >
      </div>

      <div style="flex: 1;">
        <label style="display: block; margin-bottom: 8px; color: var(--text-secondary);"> {{ t("Weight (kg)") }} </label>
        <input
            v-model="user.weight"
            type="number"
            step="0.1"
            class="input-field"
            :placeholder="t('Weight in kg')"
        >
      </div>
    </div>

    <div style="display: flex; gap: 10px;">
      <button @click="handleSave" class="btn btn-primary"> {{ t("Save") }} </button>
      <button @click="handleReturn" class="btn btn-return"> {{ t("Return") }} </button>
    </div>
  </div>
</template>

<script>
import { t } from '../i18n'
import { ref } from 'vue';

export default {
  name: 'UserForm',
  emits: ['save', 'return'],
  setup(props, { emit }) {
    const user = ref({
      age: '',
      gender: 'male',
      height: '',
      weight: ''
    });

    const handleSave = () => {
      emit('save', user.value);
    };

    const handleReturn = () => {
      emit('return');
    };

    return {
      t,
      user,
      handleSave,
      handleReturn
    };
  }
};
</script>