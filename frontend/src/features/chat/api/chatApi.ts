import axios from 'axios'

const API_BASE = '/api/ai'

export interface McpChatRequest {
  message: string
  conversationId: string
}

export interface McpChatResponse {
  message: string
  conversationId: string
  model: string
  toolsUsed: boolean
}

export async function sendMcpChat(request: McpChatRequest): Promise<McpChatResponse> {
  const { data } = await axios.post<McpChatResponse>(`${API_BASE}/mcp-chat`, request)
  return data
}

export async function clearConversation(conversationId: string): Promise<void> {
  await axios.delete(`${API_BASE}/mcp-chat/${conversationId}`)
}
