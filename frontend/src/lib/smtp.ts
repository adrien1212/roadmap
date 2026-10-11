import { queryOptions } from '@tanstack/react-query'

import { api } from '#/lib/api'

export type SmtpState = 'none' | 'untested' | 'connected' | 'not_working'
export type SmtpSecurity = 'starttls' | 'ssl'

/** The Owner's SMTP settings. The password never comes back; `passwordSaved` says whether one is stored. */
export interface SmtpSettings {
  state: SmtpState
  host: string | null
  port: number | null
  security: SmtpSecurity | null
  username: string | null
  passwordSaved: boolean
  fromName: string | null
  fromEmail: string | null
  lastError: string | null
  testedAt: string | null
}

export interface SmtpSettingsInput {
  host: string
  port: number
  security: SmtpSecurity
  username: string
  /** Empty keeps the saved password. */
  password: string
  fromName: string
  fromEmail: string
}

export const smtpSettingsQuery = queryOptions({
  queryKey: ['smtp-settings'],
  queryFn: () => api.get<SmtpSettings>('/api/smtp-settings'),
})

export const saveSmtpSettings = (input: SmtpSettingsInput) => api.put<SmtpSettings>('/api/smtp-settings', input)

export const testSmtpSettings = () => api.post<SmtpSettings>('/api/smtp-settings/test')

/** The usual port for each security mode. */
export const DEFAULT_PORT: Record<SmtpSecurity, number> = { starttls: 587, ssl: 465 }
