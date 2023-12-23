# Liquichain Payment
This module provides functionality for liquichain to receive payment from payment gateways like Paypal

## Currency conversion
This service returns the conversion rate for the different supported currencies.  This request is made via **GET** method to  `/rest/currconv`

### Sample Request
**GET** `http://localhost:8080/meveo/rest/currconv`

### Sample Response
```json
{
  "data": [
    {
      "to": {
        "value": 0.000500000,
        "currency": "LCN"
      },
      "from": {
        "value": 1,
        "currency": "EUR"
      }
    },
    {
      "to": {
        "value": 0.014750000,
        "currency": "USD"
      },
      "from": {
        "value": 1,
        "currency": "KLUB"
      }
    },
    {
      "to": {
        "value": 66.666666666666666666666667,
        "currency": "CFA"
      },
      "from": {
        "value": 1,
        "currency": "EUR"
      }
    },
    {
      "to": {
        "value": 2000.000000000,
        "currency": "EUR"
      },
      "from": {
        "value": 1,
        "currency": "LCN"
      }
    },
    {
      "to": {
        "value": 0.015000000000000000000000,
        "currency": "EUR"
      },
      "from": {
        "value": 1,
        "currency": "CFA"
      }
    },
    {
      "to": {
        "value": 67.796610169,
        "currency": "KLUB"
      },
      "from": {
        "value": 1,
        "currency": "USD"
      }
    },
    {
      "to": {
        "value": 0.016261507,
        "currency": "EUR"
      },
      "from": {
        "value": 1,
        "currency": "KLUB"
      }
    },
    {
      "to": {
        "value": 61.494915570,
        "currency": "KLUB"
      },
      "from": {
        "value": 1,
        "currency": "EUR"
      }
    }
  ],
  "sequenceId": "5056649783937025",
  "timestamp": 1703303650756
}
```

## Exchange rate history
This service is used to retrieve the exchange rate history from `KLUB` to the chosen currency. This request is made via **GET** method to  `/rest/exchangeRate/{toCurrency}` with the following path parameter:
- `toCurrency`(required): the desired currency conversion for converting `KLUB` to

### Sample Request
**GET** `http://localhost:8080/meveo/rest/exchangeRate/USD`


### Sample Response
```json
{
    "data": [
        {
            "timestamp": 1703300914340,
            "value": "0.01472",
            "percentChange": 0.0
        },
        {
            "timestamp": 1703300953044,
            "value": "0.01472",
            "percentChange": 0.0
        },
        {
            "timestamp": 1703300990803,
            "value": "0.01473",
            "percentChange": 0.0679348
        },
        {
            "timestamp": 1703301019534,
            "value": "0.01473",
            "percentChange": 0.0
        },
        {
            "timestamp": 1703301046352,
            "value": "0.01473",
            "percentChange": 0.0
        },
        {
            "timestamp": 1703301080235,
            "value": "0.01472",
            "percentChange": -0.0678887
        }
    ],
    "from": 1703217262738,
    "to": 1703303662738
}

```

## Create a payment order
This request is made via **POST** method to  `/rest/payment` with the following parameters:
- `from` (required): an object with the `currency` and `amount` of the payment that will be paid 
- `to` (required):  an object with the `currency` and `amount` of the payment that will be received
- `account` (required): the `wallet address` that will receive the payment (should not be prefixed with `0x`)
- `sequenceId` (optional): the `sequenceId` of the conversion rate during time of payment.  It ensures that the correct conversion rate is used during validation.  Otherwise, the validation will try to use the latest conversion rate.

### Sample Request
**POST** `http://localhost:8080/meveo/rest/payment`
```json
{
  "from": { "currency": "EUR", "amount": 300.0 },
  "to": { "currency": "COIN", "amount": 20000.0 },
  "account": "ac08e612D1318BC9c0Aa671A1b90199bB12Bd876",
  "sequenceId": "5056649783937025"
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
Once the payment has been completed on the payment gateway the payment can be completed by calling the capture order api at:  **POST** `/rest/payment-capture/{order id}`

In the sample response of the [Create payment order](#create_payment_order) section above, the `order id` is: `4HT32429HB5150832`

**Sample Request**

**POST** `http://localhost:8080/meveo/rest/payment-capture/4HT32429HB5150832`

**Sample Response**
```json
{
  "id": "4HT32429HB5150832",
  "status": "200",
  "result": null
}
```
