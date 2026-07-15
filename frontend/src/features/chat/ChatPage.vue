<template>
  <v-container fluid class="chat-page pa-0" style="height: calc(100vh - 64px);">
    <v-row no-gutters style="height: 100%;">
      <!-- Chat Panel -->
      <v-col cols="12" class="d-flex flex-column" style="height: 100%;">

        <!-- Header -->
        <div class="chat-header d-flex align-center justify-space-between px-6 py-4 border-b">
          <div class="d-flex align-center gap-3">
            <v-avatar color="primary" size="40">
              <v-icon color="white">mdi-robot-outline</v-icon>
            </v-avatar>
            <div>
              <div class="text-subtitle-1 font-weight-bold">Farm AI Assistant</div>
              <div class="text-caption text-medium-emphasis d-flex align-center gap-1">
                <v-icon size="10" :color="isOnline ? 'success' : 'error'">mdi-circle</v-icon>
                {{ isOnline ? 'Connected to MCP Server' : 'MCP Server offline' }}
              </div>
            </div>
          </div>
          <div class="d-flex gap-2">
            <v-btn
              variant="text"
              size="small"
              color="grey-darken-1"
              prepend-icon="mdi-refresh"
              @click="handleReset"
              :disabled="isLoading"
            >
              New Chat
            </v-btn>
          </div>
        </div>

        <!-- Message List -->
        <div ref="messageContainer" class="message-list flex-grow-1 overflow-y-auto px-4 py-4">
          <!-- Welcome state -->
          <div v-if="messages.length === 0" class="welcome-state d-flex flex-column align-center justify-center h-100 py-12">
            <v-avatar color="primary" size="72" class="mb-6">
              <v-icon color="white" size="40">mdi-robot-outline</v-icon>
            </v-avatar>
            <div class="text-h6 font-weight-bold mb-2">Farm AI Assistant</div>
            <div class="text-body-2 text-medium-emphasis text-center mb-8" style="max-width: 460px;">
              Ask me anything about your farm. I can query live data — ponds, batches,
              water quality, feed stock, harvests, and more.
            </div>

            <!-- Suggestion chips -->
            <div class="d-flex flex-wrap justify-center gap-2" style="max-width: 600px;">
              <v-chip
                v-for="suggestion in suggestions"
                :key="suggestion"
                variant="outlined"
                color="primary"
                clickable
                @click="sendSuggestion(suggestion)"
                class="suggestion-chip"
              >
                {{ suggestion }}
              </v-chip>
            </div>
          </div>

          <!-- Messages -->
          <template v-else>
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-row mb-4"
              :class="msg.role === 'user' ? 'user-row' : 'assistant-row'"
            >
              <!-- Assistant avatar -->
              <v-avatar
                v-if="msg.role === 'assistant'"
                color="primary"
                size="32"
                class="message-avatar mr-2 flex-shrink-0"
              >
                <v-icon color="white" size="18">mdi-robot-outline</v-icon>
              </v-avatar>

              <!-- Bubble -->
              <div
                class="message-bubble"
                :class="msg.role === 'user' ? 'user-bubble' : 'assistant-bubble'"
              >
                <!-- Loading indicator -->
                <div v-if="msg.isLoading" class="d-flex align-center gap-2">
                  <v-progress-circular indeterminate color="primary" size="16" width="2" />
                  <span class="text-body-2 text-medium-emphasis">Querying farm data...</span>
                </div>
                <!-- Message content -->
                <div v-else class="message-content" v-html="formatMessage(msg.content)"></div>
                <!-- Timestamp -->
                <div class="text-caption text-medium-emphasis mt-1" style="font-size: 10px !important;">
                  {{ formatTime(msg.timestamp) }}
                </div>
              </div>

              <!-- User avatar -->
              <v-avatar
                v-if="msg.role === 'user'"
                color="grey-lighten-2"
                size="32"
                class="message-avatar ml-2 flex-shrink-0"
              >
                <v-icon size="18">mdi-account</v-icon>
              </v-avatar>
            </div>
          </template>
        </div>

        <!-- Error banner -->
        <v-alert
          v-if="error"
          type="error"
          variant="tonal"
          class="mx-4 mb-2"
          closable
          @click:close="error = null"
          density="compact"
        >
          {{ error }}
        </v-alert>

        <!-- Input Bar -->
        <div class="chat-input-bar px-4 py-3 border-t">
          <div class="d-flex align-end gap-2">
            <v-textarea
              v-model="inputText"
              placeholder="Ask about your farm data..."
              variant="outlined"
              density="comfortable"
              rows="1"
              auto-grow
              max-rows="5"
              hide-details
              class="chat-input flex-grow-1"
              :disabled="isLoading"
              @keydown.enter.exact.prevent="handleSend"
              @keydown.enter.shift.exact="() => {}"
            />
            <v-btn
              :icon="isLoading ? 'mdi-stop' : 'mdi-send'"
              color="primary"
              :disabled="!inputText.trim() && !isLoading"
              @click="handleSend"
              size="large"
            />
          </div>
          <div class="text-caption text-medium-emphasis mt-1 px-1">
            Enter to send &nbsp;·&nbsp; Shift+Enter for new line
          </div>
        </div>

      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import {nextTick, onMounted, ref, watch} from 'vue'
import {useChat} from './composables/useChat'

const { messages, isLoading, error, sendMessage, resetConversation } = useChat()

const inputText = ref('')
const messageContainer = ref<HTMLElement | null>(null)
const isOnline = ref(true)

const suggestions = [
  'How many active ponds do we have?',
  'Show me the water quality for the latest batch',
  'What is the current feed stock level?',
  'Summarise the last harvest revenue',
  'List all active employees',
  'Are there any low feed stock alerts?',
  'What batches are close to harvest?',
  'Show total expenditure for the latest batch',
]

async function handleSend(): Promise<void> {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return
  inputText.value = ''
  await sendMessage(text)
}

async function sendSuggestion(text: string): Promise<void> {
  await sendMessage(text)
}

async function handleReset(): Promise<void> {
  await resetConversation()
}

function formatMessage(content: string): string {
  if (!content) return ''
  // Convert markdown-like formatting to HTML
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br />')
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// Auto-scroll to bottom on new messages
watch(messages, async () => {
  await nextTick()
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}, { deep: true })

// Check MCP server reachability on mount (best-effort)
onMounted(async () => {
  try {
    await fetch('/api/ai/models', { method: 'GET', signal: AbortSignal.timeout(3000) })
    isOnline.value = true
  } catch {
    isOnline.value = false
  }
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
}

.chat-header {
  background: var(--v-theme-surface);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.message-list {
  background: #f8f9fb;
}

.message-row {
  display: flex;
  align-items: flex-end;
}

.user-row {
  flex-direction: row-reverse;
}

.assistant-row {
  flex-direction: row;
}

.message-avatar {
  align-self: flex-end;
  margin-bottom: 4px;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 18px;
  word-wrap: break-word;
}

.user-bubble {
  background: rgb(var(--v-theme-primary));
  color: white;
  border-bottom-right-radius: 4px;
}

.assistant-bubble {
  background: white;
  color: rgba(0, 0, 0, 0.87);
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.message-content {
  line-height: 1.5;
  font-size: 0.9rem;
}

.message-content :deep(code) {
  background: rgba(0, 0, 0, 0.08);
  padding: 1px 4px;
  border-radius: 3px;
  font-family: monospace;
  font-size: 0.85em;
}

.user-bubble .message-content :deep(code) {
  background: rgba(255, 255, 255, 0.2);
}

.chat-input-bar {
  background: var(--v-theme-surface);
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.suggestion-chip {
  font-size: 0.8rem;
}

.welcome-state {
  min-height: 400px;
}
</style>
