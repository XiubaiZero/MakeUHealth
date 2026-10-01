<template>
  <div v-if="show" class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <div class="message" style="margin-bottom: 30px; font-size: 22px;">
        {{ t(message) }}
      </div>
      <button @click="handleConfirm" class="btn btn-primary"> {{ t("Confirm") }} </button>
    </div>
  </div>
</template>

<script>
import { t } from '../i18n'
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'Modal',
  props: {
    show: {
      type: Boolean,
      required: true
    },
    message: {
      type: String,
      default: ''
    }
  },
  emits: ['confirm'],
  setup(props, { emit }) {
    const handleConfirm = () => {
      emit('confirm');
    };

    const handleOverlayClick = () => {
      emit('confirm');
    };

    return {
      t,
      handleConfirm,
      handleOverlayClick
    };
  }
});
</script>