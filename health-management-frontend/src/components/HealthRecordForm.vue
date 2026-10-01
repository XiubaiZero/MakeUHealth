<template>
  <div class="card">
    <h2 style="margin-bottom: 20px; color: var(--text-primary);"> {{ t("Health Index") }} </h2>

    <div class="form-group" style="margin-bottom: 20px;">
      <label style="display: block; margin-bottom: 8px; color: var(--text-secondary);"> {{ t("Blood Pressure (mmHg)") }} </label>
      <div style="display: flex; gap: 10px;">
        <input
            v-model="record.systolic"
            type="number"
            class="input-field"
            :placeholder="t('Systolic')"
        >
        <input
            v-model="record.diastolic"
            type="number"
            class="input-field"
            :placeholder="t('Diastolic')"
        >
      </div>
    </div>

    <div class="form-group" style="margin-bottom: 20px;">
      <label style="display: block; margin-bottom: 8px; color: var(--text-secondary);"> {{ t("FBG (mmol/L)") }} </label>
      <input
          v-model="record.fbg"
          type="number"
          step="0.1"
          class="input-field"
          :placeholder="t('Fasting blood glucose')"
      >
    </div>

    <div class="form-group" style="margin-bottom: 20px;">
      <label style="display: block; margin-bottom: 8px; color: var(--text-secondary);"> {{ t("Heart Rate (bpm)") }} </label>
      <input
          v-model="record.heartRate"
          type="number"
          class="input-field"
          :placeholder="t('Heart rate')"
      >
    </div>

    <div class="form-group" style="margin-bottom: 30px;">
      <label style="display: block; margin-bottom: 8px; color: var(--text-secondary);"> {{ t("Oxyhemoglobin saturation (%)") }} </label>
      <input
          v-model="record.oxyhemoglobin"
          type="number"
          step="0.1"
          class="input-field"
          :placeholder="t('Oxyhemoglobin saturation')"
      >
    </div>

    <div style="display: flex; gap: 10px;">
      <button @click="handleSubmit" class="btn btn-primary"> {{ t("Submit") }} </button>
      <button @click="handleReset" class="btn btn-return"> {{ t("Reset") }} </button>
    </div>
  </div>
</template>

<script>
import { t } from '../i18n'
import { ref } from 'vue';

export default {
  name: 'HealthRecordForm',
  emits: ['submit', 'reset'],
  setup(props, { emit }) {
    const record = ref({
      systolic: '',
      diastolic: '',
      fbg: '',
      heartRate: '',
      oxyhemoglobin: ''
    });

    const handleSubmit = () => {
      emit('submit', record.value);
    };

    const handleReset = () => {
      record.value = {
        systolic: '',
        diastolic: '',
        fbg: '',
        heartRate: '',
        oxyhemoglobin: ''
      };
      emit('reset');
    };

    return {
      t,
      record,
      handleSubmit,
      handleReset
    };
  }
};
</script>