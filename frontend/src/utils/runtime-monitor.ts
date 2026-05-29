import { ref } from 'vue'
import type { RuntimeApiLog } from '../types/runtime'

const MAX_LOGS = 200
export const runtimeApiLogs = ref<RuntimeApiLog[]>([])

export const appendRuntimeApiLog = (log: RuntimeApiLog) => {
  runtimeApiLogs.value = [log, ...runtimeApiLogs.value].slice(0, MAX_LOGS)
}

export const clearRuntimeApiLogs = () => {
  runtimeApiLogs.value = []
}
