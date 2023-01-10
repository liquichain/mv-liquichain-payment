# Liquichain Payment
This module provides functionality for liquichain to receive payment from payment gateways like Paypal

## Create a payment order
This request is made via **POST** method to  `/rest/payment` with the following parameters:
- `from` (required): an object with the `currency` and `amount` of the payment that will be paid 
- `to` (required):  an object with the `currency` and `amount` of the payment that will be received
- `account` (required): the `wallet address` that will receive the payment (should not be prefixed with `0x`)

### Sample Request
**POST** `http://localhost:8080/meveo/rest/payment`
```json
{
  "from": { "currency": "EUR", "amount": 300.0 },
  "to": { "currency": "COIN", "amount": 20000.0 },
  "account": "ac08e612D1318BC9c0Aa671A1b90199bB12Bd876"
}
```

### Sample Response
```json
{
  "id": "4HT32429HB5150832",
  "links": [
    {
      "href": "https://api.sandbox.paypal.com/v2/checkout/orders/4HT32429HB5150832",
      "method": "GET",
      "rel": "self"
    },
    {
      "href": "https://www.sandbox.paypal.com/checkoutnow?token=4HT32429HB5150832",
      "method": "GET",
      "rel": "approve"
    },
    {
      "href": "https://api.sandbox.paypal.com/v2/checkout/orders/4HT32429HB5150832",
      "method": "PATCH",
      "rel": "update"
    },
    {
      "href": "https://api.sandbox.paypal.com/v2/checkout/orders/4HT32429HB5150832/capture",
      "method": "POST",
      "rel": "capture"
    }
  ],
  "status": "CREATED"
}
```

## Process payment on payment gateway
Once a payment order has been created, follow the link to `approve` the payment on the payment gateway.  In the example response above, go to the second link to approve the payment.

i.e. Go to `https://www.sandbox.paypal.com/checkoutnow?token=4HT32429HB5150832` then complete the payment through paypal.

## Capture the order
Once the payment has been completed on the payment gateway the payment can be completed by calling the capture order api at:  **GET** `/rest/payment-capture/{order id}`

In the sample response of the [Create payment order](#create_payment_order) section above, the `order id` is: `4HT32429HB5150832`

**Sample Request**

**GET** `http://localhost:8080/meveo/rest/payment-capture/4HT32429HB5150832`

**Sample Response**
```json
{
  "id": "4HT32429HB5150832",
  "status": "200",
  "result": null
}
```
