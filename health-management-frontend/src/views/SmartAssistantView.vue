<template>
  <div class="assistant-page">
    <p v-if="statusMessage" class="status-banner">{{ t(statusMessage) }}</p>

    <p v-if="cloudError" class="status-banner" role="status">{{ t(cloudError) }}</p>
    <p v-if="!chatReady" class="sync-notice">{{ t('Loading conversations...') }}</p>
    <p v-else-if="!online" class="sync-notice">{{ t('Chat storage is unavailable. Your draft has been kept.') }}</p>
    <div v-if="unconfirmed" class="sync-notice">
      <p>{{ t('Confirming whether your question was saved. Retry uses the same request ID.') }}</p>
      <p>{{ pendingQuestion }}</p>
      <button class="ghost-button" type="button" :disabled="working" @click="cloud.resolvePending">{{ t('Check send status') }}</button>
    </div>
    <div class="assistant-workspace">
    <aside class="conversation-panel" :class="{ 'panel-open': showConversations }" :aria-label="t('Conversations')">
      <div class="conversation-toolbar">
        <h3>{{ t('Conversations') }}</h3>
        <button class="ghost-button" type="button" :disabled="working || !online || unconfirmed" @click="newConversation">{{ t('New chat') }}</button>
      </div>
      <button class="ghost-button" type="button" :disabled="working" @click="refreshChat">{{ t('Refresh chats') }}</button>
      <button v-if="legacyAvailable" class="ghost-button" type="button" :disabled="working || !online || unconfirmed" @click="showImportDialog = true">{{ t('Import browser history') }}</button>
      <div class="conversation-items">
        <button v-for="item in conversations" :key="item.id" class="conversation-item" :class="{ active: currentConversation?.id === item.id }" type="button" :disabled="working || unconfirmed" @click="selectConversation(item)">
          <strong>{{ item.title || t('New chat') }}</strong>
          <small>{{ formatDate(item.updatedAt) }}</small>
        </button>
        <p v-if="online && !conversations.length" class="empty-hint">{{ t('No conversations yet.') }}</p>
      </div>
      <button v-if="hasMoreConversations" class="ghost-button" type="button" :disabled="working" @click="loadMoreConversations">{{ t('Load more chats') }}</button>
    </aside>
    <section class="chat-card">
      <div class="card-header">
        <div>
          <p class="eyebrow">{{ t("Assistant") }}</p>
          <h2>{{ currentConversation?.title || t("Health & Fitness Chat") }}</h2>
        </div>
        <div class="conversation-actions">
          <ConversationMemoryControl :conversation-id="currentConversation?.id || ''" :disabled="working || sending || !online" />
          <button class="ghost-button conversation-toggle" type="button" :aria-expanded="showConversations" @click="showConversations = !showConversations">{{ t('Conversations') }}</button>
          <button class="ghost-button" type="button" :disabled="working" @click="refreshChat">{{ t('Refresh chats') }}</button>
          <button class="ghost-button" type="button" :disabled="working || !online || !currentConversation || unconfirmed" @click="openRenameDialog">{{ t('Rename chat') }}</button>
          <button class="ghost-button danger-ghost" type="button" :disabled="working || !online || !currentConversation" @click="requestDeleteConversation">{{ t('Delete chat') }}</button>
        </div>
      </div>

      <div class="embedded-baseline">
        <div class="baseline-header">
          <div>
            <p class="eyebrow">{{ t("Context") }}</p>
            <h3>{{ t("Your baseline data") }}</h3>
          </div>
          <button class="ghost-button" type="button" :disabled="loadingContext" @click="loadAssistantContext">
            {{ t(loadingContext ? 'Refreshing...' : 'Refresh') }}
          </button>
        </div>

        <div v-if="!profile" class="empty-state">{{ t("No profile data found. Save your profile first.") }}</div>
        <div v-else class="baseline-grid">
          <article>
            <span>{{ t("Age") }}</span>
            <strong>{{ t(profile.age) }}</strong>
          </article>
          <article>
            <span>{{ t("Gender") }}</span>
            <strong>{{ t(profile.gender) }}</strong>
          </article>
          <article>
            <span>{{ t("Height") }}</span>
            <strong>{{ t(profile.height) }} {{ t("cm") }}</strong>
          </article>
          <article>
            <span>{{ t("Weight") }}</span>
            <strong>{{ t(profile.weight) }} {{ t("kg") }}</strong>
          </article>
        </div>

        <div class="record-brief-grid">
          <div class="record-brief">
            <h3>{{ t("Latest health record") }}</h3>
            <div v-if="latestRecord">
              <p>{{ t("Recorded on:") }} {{ t(formatDate(latestRecord.recordedAt)) }}</p>
              <p>{{ t("BP:") }} {{ t(latestRecord.systolic) }}/{{ t(latestRecord.diastolic) }} {{ t("mmHg") }}</p>
              <p>{{ t("FBG:") }} {{ t(displayValue(latestRecord.fbg)) }} {{ t("mmol/L") }}</p>
              <p>{{ t("Heart rate:") }} {{ t(displayValue(latestRecord.heartRate)) }} {{ t("bpm") }}</p>
              <p>{{ t("Oxyhemoglobin:") }} {{ t(displayValue(latestRecord.oxyhemoglobin)) }}%</p>
            </div>
            <p v-else class="empty-hint">{{ t("No health records yet.") }}</p>
          </div>

          <div class="record-brief fitness-brief">
            <div class="fitness-brief-head">
              <h3>{{ t("Fitness goal snapshot") }}</h3>
              <div ref="goalMenuRef" class="goal-switch">
                <button class="goal-switch-trigger" type="button" @click="toggleGoalTypeMenu">
                  {{ t(formatGoalTypeLabel(selectedGoalType)) }}
                </button>
                <div v-if="showGoalTypeMenu" class="goal-switch-menu">
                  <button
                    v-for="item in goalTypeOptions"
                    :key="item.value"
                    class="goal-switch-item"
                    :class="{ active: item.value === selectedGoalType }"
                    type="button"
                    @click="selectGoalType(item.value)"
                  >
                    {{ t(item.label) }}
                  </button>
                </div>
              </div>
            </div>
            <div v-if="currentGoal">
              <p>{{ t("Goal type:") }} {{ t(formatGoalTypeLabel(currentGoal.goalType)) }}</p>
              <p>{{ t("Status:") }} {{ t(currentGoal.status || 'active') }}</p>
              <p>{{ t("Current:") }} {{ t(displayValue(currentGoal.currentValue)) }}</p>
              <p>{{ t("Target:") }} {{ t(displayValue(currentGoal.targetValue)) }}</p>
              <p>{{ t("Weekly change target:") }} {{ t(displayValue(currentGoal.weeklyChange)) }}</p>
              <p>{{ t("Target date:") }} {{ t(formatDate(currentGoal.targetDate)) }}</p>
              <p>{{ t("Latest progress value:") }} {{ t(displayValue(latestGoalProgress?.currentValue)) }}</p>
              <p>{{ t("Latest progress:") }} {{ t(displayValue(latestGoalProgress?.progressPercentage)) }}%</p>
              <p>{{ t("Remaining weeks:") }} {{ t(displayValue(currentDashboard.remainingWeeks)) }}</p>
            </div>
            <p v-else class="empty-hint">{{ t("No active fitness goal yet.") }}</p>
          </div>
        </div>
      </div>

      <div ref="chatScrollRef" class="chat-stream">
        <button v-if="nextBefore !== null" class="ghost-button older-messages" type="button" :disabled="working || !online" @click="loadOlderMessages">{{ t('Load earlier messages') }}</button>
        <p v-if="!messages.length" class="empty-hint">{{ t('Hello. I focus on body health, diet nutrition, and fitness planning. Ask anything in this scope and I will answer based on your saved profile, records, and goals.') }}</p>
        <div
          v-for="message in messages"
          :key="message.id"
          class="chat-row"
          :class="message.role === 'user' ? 'user-row' : 'assistant-row'"
        >
          <label v-if="deleteSelectionMode" class="selection-checkbox" :for="`select-${message.id}`">
            <input
              :id="`select-${message.id}`"
              type="checkbox"
              :checked="selectedMessageSet.has(message.id)"
              @change="toggleMessageSelection(message.id, ($event.target as HTMLInputElement).checked)"
            />
          </label>

          <article class="chat-bubble" :class="message.role === 'user' ? 'user-bubble' : 'assistant-bubble'">
            <header class="bubble-header">
              <strong>{{ t(message.role === 'user' ? 'You' : 'Health Assistant') }}</strong>
              <div class="bubble-actions">
                <button
                  class="icon-action"
                  type="button"
                  :data-tooltip="t('Copy')"
                  :aria-label="t('Copy')"
                  :disabled="sending || deleteSelectionMode"
                  @click="copyMessage(message.content)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M9 9h9v11H9z" />
                    <path d="M6 4h9v2H8v9H6z" />
                  </svg>
                </button>
                <button
                  v-if="message.role === 'user'"
                  class="icon-action"
                  type="button"
                  :data-tooltip="t('Edit')"
                  :aria-label="t('Edit')"
                  :disabled="sending || deleteSelectionMode"
                  @click="startEditMessage(message)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 20h4l10-10-4-4L4 16z" />
                    <path d="M13 7l4 4" />
                  </svg>
                </button>
                <button
                  class="icon-action danger-icon"
                  type="button"
                  :data-tooltip="t('Delete')"
                  :aria-label="t('Delete question and answer')"
                  :disabled="sending"
                  @click="startDeleteSelectionFromMessage(message.id)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M5 7h14" />
                    <path d="M9 7V5h6v2" />
                    <path d="M8 7l1 12h6l1-12" />
                    <path d="M10 11v5" />
                    <path d="M14 11v5" />
                  </svg>
                </button>
              </div>
            </header>
            <p>{{ message.content }}</p>
            <small v-if="message.memory?.enabled" class="memory-note">{{ t('History messages used:') }} {{ message.memory.historyMessages }} · {{ t('Saved memories used:') }} {{ message.memory.longTermCount }}<span v-if="message.memory.reduced"> · {{ t('Earlier context was summarized or reduced.') }}</span></small>
            <div
              v-if="message.role === 'assistant' && message.suggestionPrompts?.length"
              class="assistant-suggestions"
            >
              <button
                v-for="prompt in message.suggestionPrompts"
                :key="`${message.id}-${prompt}`"
                class="suggestion-chip"
                type="button"
                :disabled="sending || deleteSelectionMode"
                @click="sendSuggestionPrompt(t(prompt))"
              >
                {{ t(prompt) }}
              </button>
            </div>
          </article>
        </div>

        <div v-if="sending" class="chat-row assistant-row">
          <article class="chat-bubble assistant-bubble thinking-bubble">
            <header>
              <strong>{{ t("Health Assistant") }}</strong>
            </header>
            <p>{{ t("Health Assistant is thinking...") }}</p>
            <div class="thinking-dots" aria-hidden="true">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </article>
        </div>
      </div>

      <div v-if="failedTask && messages[messages.length - 1]?.id === failedTask.questionId" class="sync-notice">
        <p>{{ t(failedTask.error || 'Generation interrupted. Please retry.') }}</p>
        <button class="ghost-button" type="button" :disabled="working || sending || !online || unconfirmed" @click="retryFailedQuestion">{{ t('Retry generation') }}</button>
      </div>

      <form v-if="!deleteSelectionMode" class="chat-input-wrap" @submit.prevent="sendMessage">
        <p v-if="editingMessageId" class="edit-banner"> {{ t("Editing a previous message.") }} <button class="inline-action-button" type="button" :disabled="sending" @click="cancelEditMessage"> {{ t("Cancel edit") }} </button>
        </p>
        <textarea
          ref="chatInputRef"
          v-model="draftMessage"
          rows="3"
          :placeholder="t('Ask about body health, diet nutrition, or fitness planning...')"
          :disabled="sending || working"
          @keydown.esc="cancelEditMessage"
        ></textarea>
        <div class="chat-actions">
          <button class="primary-button" type="submit" :disabled="sending || working || !online || unconfirmed || !draftMessage.trim()">
            {{ t(sending ? 'Thinking...' : editingMessageId ? 'Update & resend' : 'Send') }}
          </button>
        </div>
      </form>

      <div v-else class="delete-mode-wrap">
        <p class="delete-mode-hint">{{ t("Select messages on the left, then delete them.") }}</p>
        <div class="chat-actions">
          <button
            class="danger-button"
            type="button"
            :disabled="selectedMessageIds.length === 0 || working || !online || sending || unconfirmed"
            @click="openDeleteSelectedDialog"
          > {{ t("Delete") }} </button>
        </div>
      </div>
    </section>
    </div>

    <div v-if="showIntro" class="intro-overlay">
      <div class="intro-content">
        <p
          v-for="(line, index) in introLines"
          :key="line"
          class="intro-line"
          :style="{ animationDelay: `${index * 0.45}s` }"
        >
          {{ t(line) }}
        </p>
        <button class="intro-start-button" type="button" @click="closeIntro"> {{ t("Start now") }} </button>
      </div>
    </div>

    <div v-if="showRenameDialog" class="confirm-overlay" @click.self="showRenameDialog = false">
      <form class="confirm-dialog" @submit.prevent="confirmRename">
        <h3>{{ t('Rename chat') }}</h3>
        <label for="conversation-title">{{ t('Conversation title') }}</label>
        <input id="conversation-title" v-model="renameTitle" maxlength="100" required />
        <div class="confirm-actions">
          <button class="cancel-button" type="button" @click="showRenameDialog = false">{{ t('Cancel') }}</button>
          <button class="primary-button" type="submit" :disabled="working || !renameTitle.trim()">{{ t('Save') }}</button>
        </div>
      </form>
    </div>
    <div v-if="showImportDialog" class="confirm-overlay" @click.self="showImportDialog = false">
      <div class="confirm-dialog">
        <h3>{{ t('Import browser history') }}</h3>
        <p>{{ t('This creates a separate conversation. Existing cloud records and your browser backup are preserved.') }}</p>
        <div class="confirm-actions">
          <button class="cancel-button" type="button" @click="showImportDialog = false">{{ t('Cancel') }}</button>
          <button class="primary-button" type="button" :disabled="working || importing" @click="confirmImport">{{ t('Import') }}</button>
        </div>
      </div>
    </div>
    <div v-if="showEditConfirm" class="confirm-overlay" @click.self="showEditConfirm = false">
      <div class="confirm-dialog">
        <h3>{{ t('Update this question?') }}</h3>
        <p>{{ t('The original answer and all later messages will be deleted before generating a new answer.') }}</p>
        <div class="confirm-actions">
          <button class="cancel-button" type="button" @click="showEditConfirm = false">{{ t('Cancel') }}</button>
          <button class="primary-button" type="button" :disabled="working" @click="confirmEditResend">{{ t('Confirm') }}</button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteDialog" class="confirm-overlay" @click.self="cancelDeleteConversation">
      <div class="confirm-dialog">
        <h3>{{ t("Delete this conversation?") }}</h3>
        <p>{{ t("This will remove all chat messages in the current assistant conversation.") }}</p>
        <div class="confirm-actions">
          <button class="cancel-button" type="button" @click="cancelDeleteConversation">{{ t("Cancel") }}</button>
          <button class="danger-button" type="button" @click="confirmDeleteConversation">{{ t("Confirm") }}</button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteSelectedDialog" class="confirm-overlay" @click.self="cancelDeleteSelectedDialog">
      <div class="confirm-dialog">
        <h3>{{ t("Delete selected messages?") }}</h3>
        <p> {{ t("You selected") }} {{ t(selectedMessageIds.length) }} {{ t("message") }}{{ t(selectedMessageIds.length > 1 ? 's' : '') }}{{ t(". This action cannot be undone.") }} </p>
        <div class="confirm-actions">
          <button class="cancel-button" type="button" @click="cancelDeleteSelectedDialog">{{ t("Cancel") }}</button>
          <button class="danger-button" type="button" @click="confirmDeleteSelected">{{ t("Delete") }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ConversationMemoryControl from '../components/ConversationMemoryControl.vue'
import { t, dateLocale, locale } from '../i18n'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

import { conversationApi } from '../features/assistant/conversations/api'
import { createConversationSession } from '../features/assistant/conversations/session'
import { legacyImportAvailable, readLegacyHistory, markLegacyImported } from '../features/assistant/conversations/legacy'
import { buildAssistantRequest } from '../features/assistant/transport'
import type { Conversation, ConversationMessage as ChatMessage } from '../features/assistant/conversations/types'
import type { AssistantContext, GoalTypeValue } from '../features/assistant/types'
import { buildAssistantContext, countRecentFoodIntakes, findLatestHealthRecord } from '../features/assistant/context'
import { getDashboardDataByType } from '../api/fitnessGoals'
import { getFoodIntakeByUserId } from '../api/food'
import { getHealthRecordsByUserId } from '../api/healthRecords'
import type { FoodIntake, GoalDashboardData, HealthRecord, User } from '../api/types'
import { getUsers } from '../api/users'
import { getAuthStorageScope } from '../utils/auth'

const INTRO_KEY = 'smart-assistant-intro-seen-v1'
const cloud = createConversationSession(conversationApi, { scope: getAuthStorageScope, storage: sessionStorage })
const { conversations, current: currentConversation, messages, sending, working, online, error: cloudError, draft: draftMessage,
  editingId: editingMessageId, nextBefore, hasMoreConversations, failedTask, unconfirmed, pendingQuestion } = cloud
const showConversations = ref(false)
const showRenameDialog = ref(false)
const renameTitle = ref('')
const showImportDialog = ref(false)
const importing = ref(false)
const showEditConfirm = ref(false)
const editConfirmed = ref(false)
const legacyAvailable = ref(legacyImportAvailable(localStorage, getAuthStorageScope()))
const chatReady = ref(false)
const goalTypeOptions: Array<{ value: GoalTypeValue; label: string }> = [
  { value: 'muscle_gain', label: 'Muscle gain' },
  { value: 'weight_loss', label: 'Weight loss' },
  { value: 'fat_loss', label: 'Fat loss' },
]
const profile = ref<User | null>(null)
const healthRecords = ref<HealthRecord[]>([])
const foodIntakes = ref<FoodIntake[]>([])
const fitnessDashboards = ref<Record<GoalTypeValue, GoalDashboardData>>(createEmptyGoalDashboards())
const selectedGoalType = ref<GoalTypeValue>('muscle_gain')
const showGoalTypeMenu = ref(false)
const goalMenuRef = ref<HTMLElement | null>(null)
const loadingContext = ref(false)
let contextEpoch = 0
const statusMessage = ref('')
const showIntro = ref(false)
const showDeleteDialog = ref(false)
const showDeleteSelectedDialog = ref(false)
const deleteSelectionMode = ref(false)
const chatScrollRef = ref<HTMLDivElement | null>(null)
const chatInputRef = ref<HTMLTextAreaElement | null>(null)
const selectedMessageIds = ref<string[]>([])

const selectedMessageSet = computed(() => new Set(selectedMessageIds.value))

const last7DaysFoodCount = computed(() => countRecentFoodIntakes(foodIntakes.value, Date.now()))
const latestRecord = computed(() => findLatestHealthRecord(healthRecords.value))

const currentDashboard = computed(() => fitnessDashboards.value[selectedGoalType.value] ?? {})
const currentGoal = computed(() => currentDashboard.value.activeGoal ?? null)
const latestGoalProgress = computed(() => currentDashboard.value.latestProgress ?? null)

const introLines = computed(() => [
  locale.value === 'zh-CN' ? '欢迎使用健康助手。' : `Welcome, ${profile.value?.id ? `User ${profile.value.id}` : 'user'}.`,
  'I am your personal health assistant.',
  'Here you can customize personalized health services.',
  "Let's begin now.",
])

const assistantContext = computed<AssistantContext>(() => buildAssistantContext({
  profile: profile.value,
  latestRecord: latestRecord.value,
  last7DaysFoodCount: last7DaysFoodCount.value,
  fitnessDashboards: fitnessDashboards.value,
  selectedGoalType: selectedGoalType.value,
  selectedGoalLabel: formatGoalTypeLabel(selectedGoalType.value),
  goalTypeOptions,
}))

function setStatus(message: string) {
  statusMessage.value = message
  window.setTimeout(() => {
    if (statusMessage.value === message) {
      statusMessage.value = ''
    }
  }, 3000)
}

function displayValue(value?: number | string) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  return value
}

function formatDate(value?: string) {
  if (!value) {
    return 'Not provided'
  }

  return new Intl.DateTimeFormat(dateLocale(), {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

function formatGoalTypeLabel(value?: string) {
  if (!value) {
    return 'Not set'
  }

  if (value === 'weight_loss') {
    return 'Weight loss'
  }
  if (value === 'muscle_gain') {
    return 'Muscle gain'
  }
  if (value === 'fat_loss') {
    return 'Fat loss'
  }
  return value
}

function createEmptyGoalDashboards(): Record<GoalTypeValue, GoalDashboardData> {
  return {
    muscle_gain: {},
    weight_loss: {},
    fat_loss: {},
  }
}

function toggleGoalTypeMenu() {
  showGoalTypeMenu.value = !showGoalTypeMenu.value
}

function selectGoalType(goalType: GoalTypeValue) {
  selectedGoalType.value = goalType
  showGoalTypeMenu.value = false
}

function handleDocumentClick(event: MouseEvent) {
  if (!showGoalTypeMenu.value) {
    return
  }
  if (!goalMenuRef.value) {
    return
  }
  if (!goalMenuRef.value.contains(event.target as Node)) {
    showGoalTypeMenu.value = false
  }
}

function closeIntro() {
  showIntro.value = false
  localStorage.setItem(INTRO_KEY, '1')
}

async function scrollToBottom() {
  await nextTick()
  chatScrollRef.value?.scrollTo({
    top: chatScrollRef.value.scrollHeight,
    behavior: 'smooth',
  })
}

async function copyMessage(content: string) {
  try {
    await navigator.clipboard.writeText(content)
    setStatus('Copied to clipboard.')
  } catch {
    setStatus('Copy failed. Please try again.')
  }
}

async function startEditMessage(message: ChatMessage) {
  if (message.role !== 'user' || sending.value) {
    return
  }
  deleteSelectionMode.value = false
  selectedMessageIds.value = []
  showDeleteSelectedDialog.value = false
  editingMessageId.value = message.id
  draftMessage.value = message.content
  await nextTick()
  chatInputRef.value?.focus()
  chatInputRef.value?.setSelectionRange(draftMessage.value.length, draftMessage.value.length)
}

function cancelEditMessage() {
  editingMessageId.value = null
  draftMessage.value = ''
}

function requestDeleteConversation() {
  if (working.value) {
    return
  }
  showDeleteDialog.value = true
}

function cancelDeleteConversation() {
  showDeleteDialog.value = false
}

async function confirmDeleteConversation() {
  if (!await cloud.remove()) return
  deleteSelectionMode.value = false; selectedMessageIds.value = []; showDeleteSelectedDialog.value = false
  showDeleteDialog.value = false; setStatus('Conversation deleted.')
  await scrollToBottom()
}

async function loadAssistantContext() {
  const ticket = ++contextEpoch, scope = getAuthStorageScope()
  loadingContext.value = true
  try {
    const users = await getUsers()
    if (ticket !== contextEpoch || scope !== getAuthStorageScope()) return
    if (users.length === 0) {
      profile.value = null
      healthRecords.value = []
      foodIntakes.value = []
      fitnessDashboards.value = createEmptyGoalDashboards()
      return
    }

    const selected = users[users.length - 1] ?? null

    if (!selected?.id) {
      profile.value = null
      healthRecords.value = []
      foodIntakes.value = []
      fitnessDashboards.value = createEmptyGoalDashboards()
      return
    }

    profile.value = selected

    const [records, intakes, dashboardsByType] = await Promise.all([
      getHealthRecordsByUserId(selected.id),
      getFoodIntakeByUserId(selected.id),
      Promise.all(
        goalTypeOptions.map(async (item) => {
          const dashboard = await getDashboardDataByType(item.value).catch(() => null)
          return {
            goalType: item.value,
            dashboard: dashboard ?? {},
          } as const
        }),
      ),
    ])

    if (ticket !== contextEpoch || scope !== getAuthStorageScope()) return
    healthRecords.value = records
    foodIntakes.value = intakes

    const nextDashboards = createEmptyGoalDashboards()
    dashboardsByType.forEach((item) => {
      nextDashboards[item.goalType] = item.dashboard
    })
    fitnessDashboards.value = nextDashboards

    if (!nextDashboards[selectedGoalType.value]?.activeGoal) {
      const firstWithGoal = goalTypeOptions.find((item) => nextDashboards[item.value]?.activeGoal)
      if (firstWithGoal) {
        selectedGoalType.value = firstWithGoal.value
      }
    }

  } catch (error) {
    if (ticket === contextEpoch && scope === getAuthStorageScope()) setStatus(error instanceof Error ? error.message : 'Failed to load assistant context.')
  } finally {
    if (ticket === contextEpoch) loadingContext.value = false
  }
}

function resolveLinkedMessageIds(messageId: string) {
  const index = messages.value.findIndex((message) => message.id === messageId)
  if (index < 0) {
    return []
  }

  const current = messages.value[index]
  if (!current) {
    return []
  }
  const linked = [current.id]

  if (current.role === 'user') {
    const nextAssistant = messages.value.slice(index + 1).find((item) => item.role === 'assistant')
    if (nextAssistant) {
      linked.push(nextAssistant.id)
    }
  } else {
    const previousUser = [...messages.value.slice(0, index)].reverse().find((item) => item.role === 'user')
    if (previousUser) {
      linked.push(previousUser.id)
    }
  }

  return [...new Set(linked)]
}

function startDeleteSelectionFromMessage(messageId: string) {
  if (sending.value) {
    return
  }
  cancelEditMessage()
  const linkedIds = resolveLinkedMessageIds(messageId)
  if (linkedIds.length === 0) {
    return
  }
  deleteSelectionMode.value = true
  showDeleteSelectedDialog.value = false
  selectedMessageIds.value = linkedIds
}

function toggleMessageSelection(messageId: string, checked: boolean) {
  if (checked) {
    if (!selectedMessageIds.value.includes(messageId)) {
      selectedMessageIds.value.push(messageId)
    }
    return
  }
  selectedMessageIds.value = selectedMessageIds.value.filter((id) => id !== messageId)
}

function openDeleteSelectedDialog() {
  if (selectedMessageIds.value.length === 0) {
    return
  }
  showDeleteSelectedDialog.value = true
}

function cancelDeleteSelectedDialog() {
  showDeleteSelectedDialog.value = false
}

async function confirmDeleteSelected() {
  if (!await cloud.removeMessages(selectedMessageIds.value)) return
  showDeleteSelectedDialog.value = false; deleteSelectionMode.value = false; selectedMessageIds.value = []
  setStatus('Selected messages deleted.')
  await scrollToBottom()
}

async function sendSuggestionPrompt(prompt: string) {
  if (sending.value) {
    return
  }
  const nextPrompt = prompt.trim()
  if (!nextPrompt) {
    return
  }

  if (deleteSelectionMode.value) {
    deleteSelectionMode.value = false
    showDeleteSelectedDialog.value = false
    selectedMessageIds.value = []
  }

  editingMessageId.value = null
  draftMessage.value = nextPrompt
  await sendMessage()
}

async function sendMessage() {
  if (editingMessageId.value && !editConfirmed.value) { showEditConfirm.value = true; return }
  const request = buildAssistantRequest(draftMessage.value.trim(), assistantContext.value)
  await cloud.send({ ...request, mode: (import.meta.env.VITE_ASSISTANT_MODE || 'api').toLowerCase() === 'api' ? 'api' : 'local' })
  deleteSelectionMode.value = false; selectedMessageIds.value = []
  await scrollToBottom()
}
async function confirmEditResend() {
  showEditConfirm.value = false; editConfirmed.value = true
  try { await sendMessage() } finally { editConfirmed.value = false }
}
async function retryFailedQuestion() {
  const question = messages.value.find(item => item.id === failedTask.value?.questionId)
  if (question) { await startEditMessage(question); await sendMessage() }
}
async function refreshChat() { await cloud.refresh(); legacyAvailable.value = legacyImportAvailable(localStorage, getAuthStorageScope()) }
async function selectConversation(item: Conversation) {
  await cloud.select(item); showConversations.value = false
  deleteSelectionMode.value = false; selectedMessageIds.value = []
  await scrollToBottom()
}
async function newConversation() { await cloud.create(); showConversations.value = false; deleteSelectionMode.value = false; selectedMessageIds.value = []; await scrollToBottom() }
async function loadMoreConversations() {
  try { await cloud.refreshList(true) } catch { setStatus('Failed to load conversations.') }
}
async function loadOlderMessages() {
  const element = chatScrollRef.value, height = element?.scrollHeight || 0, top = element?.scrollTop || 0
  await cloud.older(); await nextTick()
  if (element) element.scrollTop = top + element.scrollHeight - height
}
function openRenameDialog() { renameTitle.value = currentConversation.value?.title || ''; showRenameDialog.value = true }
async function confirmRename() { await cloud.rename(renameTitle.value); if (currentConversation.value?.title === renameTitle.value.trim()) showRenameDialog.value = false }
async function confirmImport() {
  importing.value = true
  const scope = getAuthStorageScope(), legacy = readLegacyHistory(localStorage, scope)
  try {
    const imported = await conversationApi.importHistory(legacy, locale.value)
    if (scope !== getAuthStorageScope()) return
    markLegacyImported(localStorage, scope, legacy); legacyAvailable.value = false; showImportDialog.value = false
    await cloud.select(imported); await cloud.refreshList(); setStatus('Browser history imported.')
  } catch { setStatus('Import failed. Your browser history is unchanged.') }
  finally { importing.value = false }
}
function handleVisibilityChange() {
  if (document.visibilityState === 'visible') { cloud.resume(); void refreshChat() }
  else cloud.suspend()
}
function handleAuthStorage(event: StorageEvent) {
  if (event.key !== 'health-management-auth-session') return
  contextEpoch++; profile.value = null; healthRecords.value = []; foodIntakes.value = []; fitnessDashboards.value = createEmptyGoalDashboards()
  void refreshChat(); void loadAssistantContext()
}

onMounted(async () => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('storage', handleAuthStorage)
  await loadAssistantContext()
  await cloud.refresh()
  chatReady.value = true

  showIntro.value = !Boolean(localStorage.getItem(INTRO_KEY))
  await scrollToBottom()
})

onBeforeUnmount(() => {
  contextEpoch++
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('storage', handleAuthStorage)
  cloud.dispose()
})
</script>

<style scoped>
.assistant-workspace { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 1rem; align-items: start; }
.conversation-panel { min-width: 0; padding: 1rem; border: 1px solid #d7e5e2; border-radius: 18px; background: #f4f9f7; display: grid; gap: .75rem; }
.conversation-toolbar, .conversation-actions { display: flex; flex-wrap: wrap; align-items: center; gap: .5rem; }
.conversation-toolbar h3 { margin: 0; }
.conversation-items { display: grid; gap: .5rem; max-height: 60vh; overflow-y: auto; }
.conversation-item { display: grid; gap: .35rem; width: 100%; min-height: 52px; padding: .75rem; border: 1px solid #d7e5e2; border-radius: 12px; background: white; text-align: left; cursor: pointer; color: #12343b; overflow-wrap: anywhere; }
.conversation-item.active { border-color: #65a8a9; background: #def2ef; }
.conversation-item small { color: #647e80; }
.conversation-toggle { display: none; }
.sync-notice { margin: 0; padding: .8rem; border-radius: 12px; background: #fff6d6; color: #7d5a00; overflow-wrap: anywhere; }
.sync-notice p { margin: .3rem 0; }
.older-messages { justify-self: center; }
.chat-card { min-width: 0; }
.card-header h2 { overflow-wrap: anywhere; }
.confirm-dialog input { width: 100%; box-sizing: border-box; padding: .7rem; border: 1px solid #d7e5e2; border-radius: 10px; }
@media (max-width: 1100px) {
  .assistant-workspace { grid-template-columns: minmax(0, 1fr); }
  .conversation-panel { display: none; }
  .conversation-panel.panel-open { display: grid; }
  .conversation-toggle { display: inline-flex; }
  .conversation-actions { width: 100%; }
}

.assistant-page {
  display: grid;
  gap: 1rem;
  position: relative;
}

.eyebrow {
  margin: 0 0 0.35rem;
  color: #7d5a50;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.75rem;
  font-weight: 700;
}

h2 {
  margin: 0;
  color: #12343b;
}

.status-banner {
  margin: 0;
  padding: 0.85rem 1rem;
  border-radius: 14px;
  background: #fff6d6;
  color: #7d5a00;
  border: 1px solid #f1d57a;
}

.chat-card {
  padding: 1.35rem;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e1e7df;
  box-shadow: 0 18px 40px rgba(20, 55, 60, 0.06);
  display: grid;
  gap: 1rem;
  min-height: calc(100vh - 130px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.embedded-baseline {
  padding: 1rem;
  border-radius: 18px;
  border: 1px solid #d7e5e2;
  background:
    radial-gradient(circle at top right, rgba(84, 188, 189, 0.08), transparent 40%),
    #f4f9f7;
}

.baseline-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 0.8rem;
  margin-bottom: 0.8rem;
}

.baseline-header h3 {
  margin: 0;
  color: #1b4a52;
  font-size: 1.12rem;
}

.ghost-button {
  border: 0;
  border-radius: 14px;
  padding: 0.85rem 1.1rem;
  cursor: pointer;
  background: #edf5f4;
  color: #24555f;
  font: inherit;
}

.ghost-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.danger-ghost {
  background: #faecec;
  color: #8f2f2f;
}

.baseline-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem;
}

.baseline-grid article {
  display: grid;
  gap: 0.28rem;
  padding: 0.75rem;
  border-radius: 14px;
  border: 1px solid #dbe8e4;
  background: #f7faf8;
}

.baseline-grid span {
  color: #587176;
  font-size: 0.84rem;
}

.baseline-grid strong {
  color: #183f46;
}

.record-brief-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  align-items: stretch;
}

.record-brief {
  padding: 0.9rem;
  border-radius: 16px;
  border: 1px solid #d7e5e2;
  background: #f4f9f7;
  min-height: 228px;
}

.record-brief h3 {
  margin: 0 0 0.6rem;
  color: #1b4a52;
  font-size: 0.96rem;
}

.fitness-brief-head {
  display: block;
  margin-bottom: 0.45rem;
}

.fitness-brief-head h3 {
  margin-bottom: 0;
  padding-right: 9rem;
}

.fitness-brief {
  position: relative;
}

.goal-switch {
  position: absolute;
  top: 0.9rem;
  right: 0.9rem;
}

.goal-switch-trigger {
  border: 1px solid #c9d9d6;
  border-radius: 10px;
  padding: 0.3rem 0.58rem;
  background: #f3f8f6;
  color: #2c5961;
  font: inherit;
  font-size: 0.8rem;
  cursor: pointer;
}

.goal-switch-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 1px);
  min-width: 134px;
  padding: 0.32rem;
  border-radius: 12px;
  border: 1px solid #d5e4e1;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(20, 55, 60, 0.12);
  z-index: 10;
  display: grid;
  gap: 0.28rem;
}

.goal-switch-item {
  border: 0;
  border-radius: 9px;
  padding: 0.4rem 0.56rem;
  text-align: left;
  background: #f5f9f8;
  color: #355e65;
  font: inherit;
  font-size: 0.8rem;
  cursor: pointer;
}

.goal-switch-item.active {
  background: #e1f1ee;
  color: #1f6f7f;
}

.record-brief p {
  margin: 0.25rem 0;
  color: #4f6d73;
}

.fitness-brief > div {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 0.85rem;
}

.empty-hint,
.empty-state {
  margin: 0;
  color: #698186;
}

.chat-stream {
  min-height: 56vh;
  max-height: 66vh;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
  padding-right: 0.3rem;
}

.chat-row {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 0.45rem;
}

.assistant-row {
  justify-content: flex-start;
}

.user-row {
  justify-content: flex-end;
}

.selection-checkbox {
  margin-top: 0.6rem;
  display: inline-flex;
}

.selection-checkbox input {
  width: 1rem;
  height: 1rem;
  accent-color: #d74646;
  cursor: pointer;
}

.chat-bubble {
  padding: 0.85rem 1rem;
  border-radius: 14px;
  border: 1px solid #dbe7e4;
  max-width: 85%;
  width: fit-content;
}

.assistant-bubble {
  background: #f7faf8;
}

.thinking-bubble {
  opacity: 0.96;
}

.user-bubble {
  margin-left: 0;
  background: #e7f5f2;
  border-color: #cde5de;
}

.chat-bubble header {
  margin-bottom: 0.35rem;
}

.bubble-header {
  display: flex;
  justify-content: space-between;
  gap: 0.65rem;
  align-items: center;
}

.bubble-actions {
  display: flex;
  gap: 0.35rem;
}

.icon-action {
  position: relative;
  border: 1px solid #cfddda;
  border-radius: 10px;
  width: 1.95rem;
  height: 1.95rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f3f8f6;
  color: #677e82;
  cursor: pointer;
  transition:
    transform 0.16s ease,
    color 0.16s ease,
    border-color 0.16s ease;
}

.icon-action svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.icon-action:hover:not(:disabled) {
  transform: translateY(-1px);
  color: #315f67;
  border-color: #b8ccca;
}

.icon-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.danger-icon:hover:not(:disabled) {
  color: #d74646;
  border-color: #e6bbbb;
}

.icon-action::after {
  content: attr(data-tooltip);
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  padding: 0.2rem 0.45rem;
  border-radius: 8px;
  background: rgba(23, 44, 49, 0.92);
  color: #fff;
  font-size: 0.7rem;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transform: translateY(-2px);
  transition: opacity 0.16s ease;
}

.icon-action:hover::after {
  opacity: 1;
}

.chat-bubble strong {
  color: #183f46;
  font-size: 0.86rem;
}

.chat-bubble p {
  margin: 0;
  color: #35565d;
  line-height: 1.5;
}

.assistant-suggestions {
  margin-top: 0.65rem;
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.5rem;
  width: 100%;
}

.suggestion-chip {
  border: 1px solid #d7e3df;
  border-radius: 999px;
  padding: 0.4rem 0.72rem;
  background: #edf4f2;
  color: #2f5c64;
  font: inherit;
  font-size: 0.82rem;
  cursor: pointer;
  text-align: left;
  width: fit-content;
  max-width: 100%;
  transition:
    transform 0.15s ease,
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.suggestion-chip:hover:not(:disabled) {
  transform: translateY(-1px);
  background: #e5f1ed;
  border-color: #c6d9d3;
}

.suggestion-chip:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.thinking-dots {
  margin-top: 0.45rem;
  display: inline-flex;
  gap: 0.28rem;
  align-items: center;
}

.thinking-dots span {
  width: 0.38rem;
  height: 0.38rem;
  border-radius: 999px;
  background: #3c737b;
  opacity: 0.35;
  animation: blinkDot 1s infinite ease-in-out;
}

.thinking-dots span:nth-child(2) {
  animation-delay: 0.14s;
}

.thinking-dots span:nth-child(3) {
  animation-delay: 0.28s;
}

.chat-input-wrap {
  margin-top: 0.25rem;
  display: grid;
  gap: 0.6rem;
}

.delete-mode-wrap {
  margin-top: 0.25rem;
  display: grid;
  gap: 0.55rem;
}

.delete-mode-hint {
  margin: 0;
  color: #5a7379;
  font-size: 0.88rem;
}

.edit-banner {
  margin: 0;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d7e5e2;
  border-radius: 12px;
  background: #f3f8f7;
  color: #2d5760;
  font-size: 0.86rem;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
}

.inline-action-button {
  border: 0;
  background: transparent;
  color: #1f7a8c;
  text-decoration: underline;
  font: inherit;
  cursor: pointer;
}

.inline-action-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-input-wrap textarea {
  width: 100%;
  resize: none;
  min-height: 90px;
  max-height: 180px;
  padding: 0.8rem 0.95rem;
  border-radius: 14px;
  border: 1px solid #c9d6cf;
  background: #ffffff;
  color: #173f46;
  font: inherit;
}

.chat-input-wrap textarea:focus {
  outline: 2px solid rgba(31, 122, 140, 0.18);
  border-color: #1f7a8c;
}

.chat-actions {
  display: flex;
  justify-content: flex-end;
}

.primary-button {
  border: 0;
  border-radius: 14px;
  padding: 0.85rem 1.1rem;
  font: inherit;
  cursor: pointer;
  background: linear-gradient(135deg, #1f7a8c, #3aa17e);
  color: #fff;
}

.primary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 1450;
  background: rgba(13, 26, 31, 0.45);
  display: grid;
  place-items: center;
  padding: 1rem;
}

.confirm-dialog {
  width: min(420px, 100%);
  border-radius: 16px;
  background: #fff;
  border: 1px solid #dce6e2;
  box-shadow: 0 16px 42px rgba(19, 55, 62, 0.2);
  padding: 1rem;
}

.confirm-dialog h3 {
  margin: 0;
  color: #12343b;
}

.confirm-dialog p {
  margin: 0.7rem 0 1rem;
  color: #48646a;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.55rem;
}

.cancel-button,
.danger-button {
  border: 0;
  border-radius: 10px;
  padding: 0.55rem 0.9rem;
  font: inherit;
  cursor: pointer;
}

.cancel-button {
  background: #e9efed;
  color: #46646b;
}

.danger-button {
  background: #c34242;
  color: #fff;
}

.intro-overlay {
  position: fixed;
  inset: 0;
  z-index: 1500;
  background:
    radial-gradient(circle at top right, rgba(84, 188, 189, 0.35), transparent 40%),
    rgba(11, 25, 30, 0.86);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
}

.intro-content {
  width: min(92vw, 980px);
  display: grid;
  gap: 0.65rem;
  text-align: center;
}

.intro-line {
  margin: 0;
  font-size: clamp(1.6rem, 3.6vw, 3.2rem);
  font-weight: 800;
  letter-spacing: 0.01em;
  line-height: 1.2;
  opacity: 0;
  transform: translateY(16px);
  background: linear-gradient(90deg, #b8fff8, #ffe6a9, #d4e8ff, #b8fff8);
  background-size: 220% 220%;
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  animation:
    introFadeIn 0.9s ease forwards,
    introGradient 5s linear infinite;
}

.intro-start-button {
  justify-self: center;
  margin-top: 1rem;
  border: 0;
  border-radius: 14px;
  padding: 0.85rem 1.35rem;
  color: #12343b;
  background: linear-gradient(135deg, #c9fff6, #fff0bf);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

@keyframes introFadeIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes introGradient {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 100% 50%;
  }
}

@keyframes blinkDot {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.3;
  }
  40% {
    transform: translateY(-2px);
    opacity: 0.95;
  }
}

@media (max-width: 1080px) {
  .chat-stream {
    min-height: 50vh;
    max-height: 60vh;
  }
}

@media (max-width: 720px) {
  .chat-card {
    min-width: 0;
    grid-template-columns: minmax(0, 1fr);
    padding: 14px 12px;
    border-radius: 20px;
    gap: 14px;
  }

  .card-header,
  .baseline-header {
    flex-wrap: wrap;
    gap: 8px;
  }

  .card-header > div,
  .baseline-header > div {
    min-width: 0;
    flex: 1;
  }

  .card-header > .conversation-actions {
    flex: 1 0 100%;
  }

  .card-header h2 {
    font-size: 1.125rem;
    line-height: 1.4;
  }

  .ghost-button {
    min-height: 44px;
    padding: 10px;
    font-size: 0.75rem;
  }

  .embedded-baseline {
    min-width: 0;
    padding: 12px;
  }

  .baseline-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .record-brief-grid,
  .fitness-brief > div {
    grid-template-columns: 1fr;
  }

  .record-brief {
    min-height: 0;
    padding: 12px;
    font-size: 0.85rem;
    line-height: 1.5;
  }

  .fitness-brief-head {
    margin-bottom: 0.7rem;
  }

  .fitness-brief-head h3 {
    padding-right: 0;
    padding-top: 2.3rem;
  }

  .goal-switch {
    top: 0.75rem;
    right: 0.75rem;
  }

  .edit-banner {
    align-items: start;
    flex-direction: column;
  }

  .chat-bubble {
    min-width: 0;
    max-width: 92%;
    overflow-wrap: anywhere;
  }

  .bubble-header {
    flex-wrap: wrap;
  }

  .icon-action {
    width: 44px;
    height: 44px;
  }
}
</style>
