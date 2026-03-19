import request from './request'

const SESSION_KEY = 'behavior_session_id'
const FLUSH_INTERVAL = 5000
const MAX_BATCH = 20

const queue = []
let flushTimer = null

const randomId = () => {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

const getSessionId = () => {
  let sessionId = localStorage.getItem(SESSION_KEY)
  if (!sessionId) {
    sessionId = randomId()
    localStorage.setItem(SESSION_KEY, sessionId)
  }
  return sessionId
}

const ensureFlushTimer = () => {
  if (flushTimer) return
  flushTimer = window.setInterval(() => {
    flushBehaviorEvents()
  }, FLUSH_INTERVAL)
}

export const trackBehavior = (eventType, payload = {}) => {
  if (!eventType) return
  queue.push({
    eventType,
    recipeId: payload.recipeId || null,
    sourcePage: payload.sourcePage || null,
    sceneCode: payload.sceneCode || null,
    stepNumber: payload.stepNumber || null,
    durationMs: payload.durationMs || null,
    extra: payload.extra || null
  })

  if (queue.length >= MAX_BATCH) {
    flushBehaviorEvents()
  } else {
    ensureFlushTimer()
  }
}

export const flushBehaviorEvents = async () => {
  if (!queue.length) return
  const events = queue.splice(0, queue.length)
  try {
    await request.post('/analytics/events/batch', {
      sessionId: getSessionId(),
      events
    })
  } catch (error) {
    queue.unshift(...events)
    if (queue.length > 100) {
      queue.length = 100
    }
  }
}

if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      flushBehaviorEvents()
    }
  })
}
