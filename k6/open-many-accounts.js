import http from 'k6/http';
import { check } from 'k6';

export const options = {
	  discardResponseBodies: true,
	  scenarios: {
	    accounts: {
	      executor: 'ramping-vus',
	      startVUs: 10,
	      stages: [
	        { duration: '10s', target: 300 },
	        { duration: '60s', target: 1000 },
 	        { duration: '10s', target: 10 },
	      ],
	      gracefulRampDown: '1s',
	    },
	  },
	};

export default function () {
  const id = Math.random() * 16
  const payload = JSON.stringify({cpf: id, name: id});
  const params = { headers: { 'Content-Type': 'application/json' } };
  const resp1 = http.put('http://0.0.0.0:8080/accounts/' + id, payload, params);
  check(resp1, {
    'is status 200': (r) => r.status === 200,
  });
};

function create_UUID(){
    var dt = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (dt + Math.random()*16)%16 | 0;
        dt = Math.floor(dt/16);
        return (c=='x' ? r :(r&0x3|0x8)).toString(16);
    });
    // console.log(uuid);
    return uuid;
}
