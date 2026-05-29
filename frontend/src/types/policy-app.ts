import type { PageVO } from './user'

export type AppResourceVO = {
  id: number
  appCode: string
  appName: string
  resourceType: string
  accessUrl: string
  securityLevel: number
  requiredTrustLevel: number
  status: string
  description: string
  metadata: string
  createdAt: string
  updatedAt: string
}

export type AuthMethodVO = {
  id: number
  methodCode: string
  methodName: string
  methodType: string
  compareMode: string
  enabled: boolean
  sortOrder: number
  description: string
  createdAt: string
  updatedAt: string
}

export type AuthPolicyVO = {
  id: number
  policyCode: string
  policyName: string
  sceneType: string
  appResourceId: number | null
  userScope: string
  deviceScope: string
  authChain: string
  minTrustLevel: number
  sessionTtlSeconds: number
  actionOnFail: string
  status: string
  versionNo: number
  createdBy: number
  createdAt: string
  updatedAt: string
  appResource: AppResourceVO | null
}

export type SecurityBaselineVO = {
  id: number
  baselineCode: string
  baselineName: string
  targetType: string
  targetScope: string
  ruleConfig: string
  riskLevel: number
  actionOnHit: string
  status: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

export type PolicyPublishRecordVO = {
  id: number
  policyType: string
  policyId: number
  versionNo: number
  targetType: string
  targetId: number | null
  publishStatus: string
  executeResult: string
  failReason: string
  publishedBy: number
  publishedAt: string
  ackAt: string
}

export type AppListResponse = PageVO<AppResourceVO>
export type MethodListResponse = PageVO<AuthMethodVO>
export type PolicyListResponse = PageVO<AuthPolicyVO>
export type BaselineListResponse = PageVO<SecurityBaselineVO>
export type PublishRecordListResponse = PageVO<PolicyPublishRecordVO>

