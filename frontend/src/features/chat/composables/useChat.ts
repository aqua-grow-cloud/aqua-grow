import {ref, type Ref} from 'vue'
import {v4 as uuidv4} from 'uuid'
import {clearConversation, sendMcpChat} from '../api/chatApi'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  isLoading?: boolean
}

interface UseChatReturn {
  messages: Ref<ChatMessage[]>
  isLoading: Ref<boolean>
  error: Ref<string | null>
  conversationId: Ref<string>
  sendMessage: (text: string) => Promise<void>
  resetConversation: () => Promise<void>
}

export function useChat(): UseChatReturn {
  const conversationId = ref<string>(uuidv4())
  const messages = ref<ChatMessage[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  async function sendMessage(text: string): Promise<void> {
    if (!text.trim() || isLoading.value) return

    error.value = null

    // Append user message immediately
    const userMsg: ChatMessage = {
      id: uuidv4(),
      role: 'user',
      content: text.trim(),
      timestamp: new Date()
    }
    messages.value.push(userMsg)

    // Append loading placeholder for assistant
    const loadingMsg: ChatMessage = {
      id: uuidv4(),
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isLoading: true
    }
    messages.value.push(loadingMsg)

    isLoading.value = true

    try {
      const response = await sendMcpChat({
        message: text.trim(),
        conversationId: conversationId.value
      })

      // Replace loading placeholder with actual response
      const idx = messages.value.findIndex(m => m.id === loadingMsg.id)
      if (idx !== -1) {
        messages.value[idx] = {
          id: loadingMsg.id,
          role: 'assistant',
          content: response.message,
          timestamp: new Date(),
          isLoading: false
        }
      }
    } catch (err: unknown) {
      // Remove loading placeholder and show error
      messages.value = messages.value.filter(m => m.id !== loadingMsg.id)
      error.value = 'Failed to get a response. Make sure the AI service is running.'
    } finally {
      isLoading.value = false
    }
  }

  async function resetConversation(): Promise<void> {
    try {
      await clearConversation(conversationId.value)
    } catch {
      // Silently ignore if the session didn't exist server-side
    }
    conversationId.value = uuidv4()
    messages.value = []
    error.value = null
  }

  return { messages, isLoading, error, conversationId, sendMessage, resetConversation }
}
