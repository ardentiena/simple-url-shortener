import http from 'k6/http';
import { check } from 'k6';
import execution from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const TOTAL_SEEDED_LINKS = 100000;
const VIRTUAL_USERS = Number(__ENV.VIRTUAL_USERS || 50);
const PREFILL_VIRTUAL_USERS = 20;

export const options = {
  discardResponseBodies: true,
  systemTags: ['status', 'method', 'scenario', 'expected_response', 'check', 'error', 'error_code'],
  scenarios: {
    prefill: {
      executor: 'shared-iterations',
      vus: PREFILL_VIRTUAL_USERS,
      iterations: TOTAL_SEEDED_LINKS,
      maxDuration: '60s',
      exec: 'prefillAlias',
    },
    warmup: {
      executor: 'constant-vus',
      vus: VIRTUAL_USERS,
      duration: '30s',
      startTime: '60s',
      exec: 'requestRedirect',
    },
    measured: {
      executor: 'constant-vus',
      vus: VIRTUAL_USERS,
      duration: '60s',
      startTime: '90s',
      exec: 'requestRedirect',
    },
  },
  thresholds: {
    'http_req_duration{scenario:measured}': ['p(95)<5000'],
    'http_reqs{scenario:measured}': ['count>0'],
    'http_req_failed{scenario:measured}': ['rate<0.01'],
    'checks{scenario:measured}': ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function formatAlias(aliasNumber) {
  return 'bench' + String(aliasNumber).padStart(6, '0');
}

function requestAlias(aliasNumber) {
  const redirectResponse = http.get(`${BASE_URL}/${formatAlias(aliasNumber)}`, { redirects: 0 });

  check(redirectResponse, {
    'status is 302': (response) => response.status === 302,
    'location header is present': (response) => Boolean(response.headers['Location']),
  });
}

export function prefillAlias() {
  const aliasNumber = execution.scenario.iterationInTest + 1;
  requestAlias(aliasNumber);
}

export function requestRedirect() {
  const aliasNumber = Math.floor(Math.random() * TOTAL_SEEDED_LINKS) + 1;
  requestAlias(aliasNumber);
}
