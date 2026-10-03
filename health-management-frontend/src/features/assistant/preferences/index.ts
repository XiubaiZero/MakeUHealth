import { getAuthStorageScope } from '../../../utils/auth'
import { preferencesApi } from './api'
import { createPreferencesState } from './state'
export const assistantPreferences = createPreferencesState(preferencesApi, getAuthStorageScope)
