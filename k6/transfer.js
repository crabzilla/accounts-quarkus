import http from 'k6/http';
import { check } from 'k6';

export default function () {
  const openAccount1 = JSON.stringify({cpf: "cpf1", name: "name1"});
  const depositAccount1 = JSON.stringify({amount: 110});
  const openAccount2 = JSON.stringify({cpf: "cpf2", name: "name2"});
  const requestTransfer = JSON.stringify({
    amount: 100,
    fromAccountId: "25a4b17c-3512-11ec-8d3d-0242ac130003",
    toAccountId: "25a4b17c-3512-11ec-8d3d-0242ac130004"
  });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const resp1 =
   http.put('http://0.0.0.0:8080/accounts/25a4b17c-3512-11ec-8d3d-0242ac130003', openAccount1, params);
  const resp2
  = http.post('http://0.0.0.0:8080/accounts/25a4b17c-3512-11ec-8d3d-0242ac130003/deposit', depositAccount1, params);
  const resp3
  = http.put('http://0.0.0.0:8080/accounts/25a4b17c-3512-11ec-8d3d-0242ac130004', openAccount2, params);
  const resp4
  = http.put('http://0.0.0.0:8080/transfers/25a4b17c-3512-11ec-8d3d-0242ac130005', requestTransfer, params);

   check(resp1, {
     'is status 200': (r) => r.status === 200,
   });
   check(resp2, {
     'is status 200': (r) => r.status === 200,
   });
   check(resp3, {
     'is status 200': (r) => r.status === 200,
   });
   check(resp4, {
     'is status 200': (r) => r.status === 200,
   });


};

