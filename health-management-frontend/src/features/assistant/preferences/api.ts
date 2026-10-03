import apiClient from '../../../api/client'
export type AssistantPreferences = { enterSendEnabled: boolean; revision: number }
export type PreferencesApi = { get(): Promise<AssistantPreferences>; update(enabled: boolean, expectedRevision: number): Promise<AssistantPreferences> }
export const preferencesApi: PreferencesApi = {
  async get() { return (await apiClient.get<AssistantPreferences>('/assistant/preferences')).data },
  async update(enterSendEnabled, expectedRevision) { return (await apiClient.patch<AssistantPreferences>('/assistant/preferences', { enterSendEnabled, expectedRevision })).data },
}
