# Admin API Reference

> 모든 Admin API는 `X-API-Key` 헤더 인증 필수.
> Base URL: `https://mud-production-5786.up.railway.app`

---

## 크롤링

### POST /api/admin/crawl
전체 크롤러 비동기 실행 트리거.

```bash
curl -X POST -H "X-API-Key: {KEY}" {BASE}/api/admin/crawl
```

응답: `{ "status": "크롤링 시작됨 - 백그라운드에서 실행 중" }`

---

## 분석

### POST /api/admin/analyze
PENDING 상태 아이템 분석 트리거.

```bash
curl -X POST -H "X-API-Key: {KEY}" {BASE}/api/admin/analyze
```

---

## Scoring / 재채점

### POST /api/admin/rescore
기존 아이템 100점 체계 재채점. 날짜 범위 지정 가능.

```bash
# 전체
curl -X POST -H "X-API-Key: {KEY}" {BASE}/api/admin/rescore

# 특정 날짜 이후만
curl -X POST -H "X-API-Key: {KEY}" "{BASE}/api/admin/rescore?from=2026-03-29"

# 특정 기간
curl -X POST -H "X-API-Key: {KEY}" "{BASE}/api/admin/rescore?from=2026-03-28&to=2026-03-29"
```

### GET /api/admin/rescore/status
재채점 진행 상황 조회.

```bash
curl -H "X-API-Key: {KEY}" {BASE}/api/admin/rescore/status
```

응답: `{ "processed": 1000, "total": 3500, "status": "IN_PROGRESS" }`

---

## 캐시

### POST /api/admin/flush-cache
CacheManager를 통한 애플리케이션 캐시 클리어. (Redis 분산 락 등은 보존)

```bash
curl -X POST -H "X-API-Key: {KEY}" {BASE}/api/admin/flush-cache
```

---

## 크롤러 모니터링

### GET /api/admin/crawlers/status
전체 크롤러 최신 상태 조회.

```bash
curl -H "X-API-Key: {KEY}" {BASE}/api/admin/crawlers/status
```

응답:
```json
{
  "crawlers": [
    {
      "source": "GITHUB",
      "status": "OK",
      "lastRunAt": "2026-03-29T14:30:00",
      "lastSuccessAt": "2026-03-29T14:30:00",
      "lastFailedAt": null,
      "itemsCollected": 12,
      "errorMessage": null
    }
  ],
  "summary": { "total": 29, "ok": 27, "failed": 2 }
}
```

### GET /api/admin/crawlers/history
기간별 크롤러 실행 이력 조회.

```bash
# 특정 날짜
curl -H "X-API-Key: {KEY}" "{BASE}/api/admin/crawlers/history?date=2026-03-29"

# 기간 + 실패만
curl -H "X-API-Key: {KEY}" "{BASE}/api/admin/crawlers/history?from=2026-03-28&to=2026-03-29&status=FAILED"
```

파라미터:
- `date`: 단일 날짜
- `from`, `to`: 기간 범위
- `status`: `OK` 또는 `FAILED` (선택)
- `source`: 특정 소스만 (선택)

---

## 주간 리포트

### GET /api/reports/weekly
최신 주간 리포트 조회. (Public API, 인증 불필요)

```bash
curl {BASE}/api/reports/weekly
curl "{BASE}/api/reports/weekly?week=2026-03-16"
```

### POST /api/admin/reports/generate
주간 리포트 수동 생성.

```bash
curl -X POST -H "X-API-Key: {KEY}" "{BASE}/api/admin/reports/generate?date=2026-03-23"
```

### POST /api/admin/reports/regenerate
기존 주간 리포트 삭제 후 재생성.

```bash
curl -X POST -H "X-API-Key: {KEY}" "{BASE}/api/admin/reports/regenerate?date=2026-03-23"
```

---

## 데일리 다이제스트

### POST /api/admin/digest/test-email
SMTP 설정 테스트. 지정 이메일로 테스트 메일 발송.

```bash
curl -X POST -H "X-API-Key: {KEY}" "{BASE}/api/admin/digest/test-email?to=me@gmail.com"
```

### POST /api/admin/digest/send-now
Quartz 스케줄러 안 기다리고 즉시 다이제스트 발송.

```bash
curl -X POST -H "X-API-Key: {KEY}" {BASE}/api/admin/digest/send-now
```

응답: `{ "status": "발송 완료", "itemCount": 10, "subscriberCount": 3, "sentCount": 3 }`

### GET /api/admin/digest/subscribers
구독자 목록 조회.

```bash
curl -H "X-API-Key: {KEY}" {BASE}/api/admin/digest/subscribers
```

---

## API 사용량 *(예정 — B34)*

### GET /api/admin/usage/summary
Anthropic API 사용량 조회.

```bash
curl -H "X-API-Key: {KEY}" "{BASE}/api/admin/usage/summary?days=7"
```

---

## 구독 API (Public)

### POST /api/digest/subscribe
이메일 구독 신청. Double opt-in (인증 이메일 발송).

```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}' \
  {BASE}/api/digest/subscribe
```

### GET /api/digest/verify?token={token}
이메일 인증 완료.

### GET /api/digest/unsubscribe?token={token}
구독 해지.
