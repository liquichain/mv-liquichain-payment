import EndpointInterface from "#{API_BASE_URL}/api/rest/endpoint/EndpointInterface.js";

// the request schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this is used to validate and parse the request parameters
const requestSchema = {
  "title" : "exchangeRateRequest",
  "id" : "exchangeRateRequest",
  "default" : "Schema definition for exchangeRate",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object",
  "properties" : {
    "epsilon" : {
      "title" : "epsilon",
      "id" : "exchangeRate_epsilon",
      "type" : "string",
      "minLength" : 1
    },
    "from" : {
      "title" : "from",
      "id" : "exchangeRate_from",
      "type" : "string",
      "minLength" : 1
    },
    "to" : {
      "title" : "to",
      "id" : "exchangeRate_to",
      "type" : "string",
      "minLength" : 1
    },
    "maxValues" : {
      "title" : "maxValues",
      "id" : "exchangeRate_maxValues",
      "type" : "string",
      "minLength" : 1
    }
  }
}

// the response schema, this should be updated
// whenever changes to the endpoint parameters are made
// this is important because this could be used to parse the result
const responseSchema = {
  "title" : "exchangeRateResponse",
  "id" : "exchangeRateResponse",
  "default" : "Schema definition for exchangeRate",
  "$schema" : "http://json-schema.org/draft-07/schema",
  "type" : "object",
  "properties" : {
    "result" : {
      "title" : "result",
      "type" : "string",
      "minLength" : 1
    }
  }
}

// should contain offline mock data, make sure it adheres to the response schema
const mockResult = {};

class exchangeRate extends EndpointInterface {
	constructor() {
		// name and http method, these are inserted when code is generated
		super("exchangeRate", "GET");
		this.requestSchema = requestSchema;
		this.responseSchema = responseSchema;
		this.mockResult = mockResult;
	}

	getRequestSchema() {
		return this.requestSchema;
	}

	getResponseSchema() {
		return this.responseSchema;
	}
}

export default new exchangeRate();