import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Mail } from 'lucide-react'
import { toast } from 'sonner'

import { FieldError } from '#/components/field-error'
import { Button } from '#/components/ui/button'
import { Input } from '#/components/ui/input'
import { Label } from '#/components/ui/label'
import { formatInstant } from '#/lib/format'
import {
  DEFAULT_PORT,
  saveSmtpSettings,
  smtpSettingsQuery,
  testSmtpSettings,
  type SmtpSecurity,
  type SmtpSettings,
} from '#/lib/smtp'
import { cn } from '#/lib/utils'

const hint = 'm-0 text-[13px] text-muted-foreground'
const label = 'text-[13px] font-medium text-body'
const segButton =
  'h-[30px] cursor-pointer rounded-[6px] px-3 text-[13px] font-medium text-muted-foreground transition-colors hover:text-foreground aria-checked:bg-background aria-checked:font-semibold aria-checked:text-foreground aria-checked:shadow-soft'

/** "Email to Readers": the Owner's SMTP server (design/roadmap-ui.html, Settings). */
export function SmtpSettingsForm({ settings, brandName }: { settings: SmtpSettings; brandName: string | null }) {
  const queryClient = useQueryClient()
  const [host, setHost] = useState(settings.host ?? '')
  const [port, setPort] = useState(String(settings.port ?? DEFAULT_PORT.starttls))
  const [security, setSecurity] = useState<SmtpSecurity>(settings.security ?? 'starttls')
  const [username, setUsername] = useState(settings.username ?? '')
  const [password, setPassword] = useState('')
  const [replacingPassword, setReplacingPassword] = useState(!settings.passwordSaved)
  const [fromName, setFromName] = useState(settings.fromName ?? '')
  const [fromEmail, setFromEmail] = useState(settings.fromEmail ?? '')
  const [error, setError] = useState<string | null>(null)

  const onSaved = (saved: SmtpSettings) => {
    queryClient.setQueryData(smtpSettingsQuery.queryKey, saved)
    // The Public page's subscribe box depends on the state.
    queryClient.invalidateQueries({ queryKey: ['public'] })
    setError(null)
  }

  const save = useMutation({
    mutationFn: () =>
      saveSmtpSettings({ host, port: Number(port), security, username, password, fromName, fromEmail }),
    onSuccess: (saved) => {
      onSaved(saved)
      setPassword('')
      setReplacingPassword(!saved.passwordSaved)
      toast('Saved. Send a test email to turn on subscriptions.')
    },
    onError: (e) => setError(e.message),
  })

  const test = useMutation({
    mutationFn: testSmtpSettings,
    onSuccess: (tested) => {
      onSaved(tested)
      if (tested.state === 'connected') toast('Test email sent to you. Readers can subscribe now.')
    },
    onError: (e) => setError(e.message),
  })

  function changeSecurity(next: SmtpSecurity) {
    // Follow the usual port when the Owner hasn't picked another one.
    if (port === String(DEFAULT_PORT[security])) setPort(String(DEFAULT_PORT[next]))
    setSecurity(next)
  }

  return (
    <form
      aria-labelledby="smtp-t"
      className="col-span-full flex flex-col gap-5 rounded-lg border border-border p-6"
      onSubmit={(e) => {
        e.preventDefault()
        save.mutate()
      }}
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 id="smtp-t" className="text-lg font-semibold">
            Email to Readers
          </h2>
          <p className={cn(hint, 'mt-1')}>
            Readers who subscribe get an email when an Item becomes Shipped. It's sent through your own SMTP server,
            from your address.
          </p>
        </div>
        <SmtpStateLine settings={settings} />
      </div>

      <div className="grid grid-cols-1 gap-3 min-[640px]:grid-cols-[minmax(0,1fr)_120px]">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="s-host" className={label}>
            SMTP host
          </Label>
          <Input id="s-host" value={host} onChange={(e) => setHost(e.target.value)} placeholder="smtp.example.com" />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="s-port" className={label}>
            Port
          </Label>
          <Input
            id="s-port"
            value={port}
            inputMode="numeric"
            onChange={(e) => setPort(e.target.value.replace(/\D/g, ''))}
            className="tabular-nums"
          />
        </div>
        <div className="col-span-full flex flex-col gap-1.5">
          <span id="l-sec" className={label}>
            Security
          </span>
          <div role="radiogroup" aria-labelledby="l-sec" className="inline-flex w-fit gap-0.5 rounded-md bg-surface-card p-[3px]">
            {(['starttls', 'ssl'] as const).map((value) => (
              <button
                key={value}
                type="button"
                role="radio"
                aria-checked={security === value}
                onClick={() => changeSecurity(value)}
                className={segButton}
              >
                {value === 'starttls' ? 'STARTTLS' : 'SSL/TLS'}
              </button>
            ))}
          </div>
          <p className={hint}>Usually STARTTLS on port 587, or SSL/TLS on port 465.</p>
        </div>
        <div className="col-span-full flex flex-col gap-1.5">
          <Label htmlFor="s-user" className={label}>
            Username <span className="font-normal text-muted-foreground">(if your server needs one)</span>
          </Label>
          <Input id="s-user" value={username} autoComplete="off" onChange={(e) => setUsername(e.target.value)} />
        </div>
        <div className="col-span-full flex flex-col gap-1.5">
          <Label htmlFor="s-pass" className={label}>
            Password
          </Label>
          {replacingPassword ? (
            <Input
              id="s-pass"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="App password or SMTP key"
            />
          ) : (
            <div className="flex h-10 items-center gap-2 rounded-md border border-border bg-surface-soft pr-1.5 pl-3.5 text-[15px] text-body">
              <span aria-label="Password saved" className="flex-1 tracking-[2px]">
                ••••••••••
              </span>
              <span className="text-[13px] font-medium text-muted-foreground">Saved, encrypted</span>
              <Button type="button" variant="ghost" size="sm" onClick={() => setReplacingPassword(true)}>
                Replace
              </Button>
            </div>
          )}
          <p className={hint}>Stored encrypted. It's never shown again after you save it.</p>
        </div>
        <div className="col-span-full flex flex-col gap-1.5">
          <Label htmlFor="s-from-name" className={label}>
            Sender name
          </Label>
          <Input
            id="s-from-name"
            value={fromName}
            maxLength={100}
            onChange={(e) => setFromName(e.target.value)}
            placeholder={brandName ?? 'Your product'}
          />
        </div>
        <div className="col-span-full flex flex-col gap-1.5">
          <Label htmlFor="s-from" className={label}>
            Sender email
          </Label>
          <Input
            id="s-from"
            type="email"
            value={fromEmail}
            onChange={(e) => setFromEmail(e.target.value)}
            placeholder="updates@example.com"
          />
          <p className={hint}>Readers see it as the sender. Use an address your SMTP server is allowed to send from.</p>
        </div>
      </div>

      {error && <FieldError>{error}</FieldError>}

      <div className="flex flex-wrap gap-3">
        <Button type="submit" disabled={save.isPending}>
          Save
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={test.isPending || settings.state === 'none'}
          onClick={() => test.mutate()}
        >
          <Mail aria-hidden />
          {test.isPending ? 'Sending…' : 'Send test email'}
        </Button>
      </div>
    </form>
  )
}

function SmtpStateLine({ settings }: { settings: SmtpSettings }) {
  const base = 'inline-flex items-center gap-1.5 text-[13px] font-medium'
  const dot = 'size-1.5 rounded-full'
  switch (settings.state) {
    case 'connected':
      return (
        <span className={cn(base, 'text-success-text')} suppressHydrationWarning>
          <span aria-hidden className={cn(dot, 'bg-success')} />
          Connected · test email sent {settings.testedAt && formatInstant(settings.testedAt)}
        </span>
      )
    case 'not_working':
      return (
        <span className="flex max-w-[420px] flex-col items-end gap-1 text-right">
          <span className={cn(base, 'text-error-text')}>
            <span aria-hidden className={cn(dot, 'bg-error')} />
            Not working
          </span>
          {settings.lastError && <span className="text-[13px] text-error-text">{settings.lastError}</span>}
        </span>
      )
    case 'untested':
      return (
        <span className={cn(base, 'text-muted-foreground')}>
          <span aria-hidden className={cn(dot, 'bg-muted-soft')} />
          Saved · send a test email
        </span>
      )
    default:
      return (
        <span className={cn(base, 'text-muted-foreground')}>
          <span aria-hidden className={cn(dot, 'bg-muted-soft')} />
          Not set up. Readers can follow with RSS only.
        </span>
      )
  }
}
