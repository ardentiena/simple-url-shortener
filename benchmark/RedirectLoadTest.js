import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const TOTAL_SEEDED_LINKS = 100000;
const VIRTUAL_USERS = Number(__ENV.VIRTUAL_USERS || 50);

export const options = {
    scenarios: {
        warmup: {
            executor: 'constant-vus',
            vus: VIRTUAL_USERS,
            duration: '30s',
            exec: 'requestRedirect',
        },
        measured: {
            executor: 'constant-vus',
            vus: VIRTUAL_USERS,
            duration: '60s',
            startTime: '30s',
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

function pickRandomAlias() {
    const aliasNumber = Math.floor(Math.random() * TOTAL_SEEDED_LINKS) + 1;
    return 'bench' + String(aliasNumber).padStart(6, '0');
}

export function requestRedirect() {
    const alias = pickRandomAlias();
    const redirectResponse = http.get(`${BASE_URL}/${alias}`, { redirects: 0 });

    check(redirectResponse, {
        'status is 302': (response) => response.status === 302,
        'location header is present': (response) => Boolean(response.headers['Location']),
    });
}