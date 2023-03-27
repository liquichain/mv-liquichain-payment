const exchangeRate = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/exchangeRate/${parameters.fromCurrency}/${parameters.toCurrency}`, baseUrl);
	if (parameters.fromDate !== undefined) {
		url.searchParams.append('fromDate', parameters.fromDate);
	}

	if (parameters.toDate !== undefined) {
		url.searchParams.append('toDate', parameters.toDate);
	}

	if (parameters.maxValues !== undefined) {
		url.searchParams.append('maxValues', parameters.maxValues);
	}

	if (parameters.fromDate !== undefined) {
		url.searchParams.append('fromDate', parameters.fromDate);
	}

	if (parameters.toDate !== undefined) {
		url.searchParams.append('toDate', parameters.toDate);
	}

	if (parameters.maxValues !== undefined) {
		url.searchParams.append('maxValues', parameters.maxValues);
	}

	return fetch(url.toString(), {
		method: 'GET'
	});
}

const exchangeRateForm = (container) => {
	const html = `<form id='exchangeRate-form'>
		<div id='exchangeRate-fromCurrency-form-field'>
			<label for='fromCurrency'>fromCurrency</label>
			<input type='text' id='exchangeRate-fromCurrency-param' name='fromCurrency'/>
		</div>
		<div id='exchangeRate-toCurrency-form-field'>
			<label for='toCurrency'>toCurrency</label>
			<input type='text' id='exchangeRate-toCurrency-param' name='toCurrency'/>
		</div>
		<div id='exchangeRate-from-form-field'>
			<label for='from'>from</label>
			<input type='text' id='exchangeRate-from-param' name='from'/>
		</div>
		<div id='exchangeRate-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='exchangeRate-to-param' name='to'/>
		</div>
		<div id='exchangeRate-limit-form-field'>
			<label for='limit'>limit</label>
			<input type='text' id='exchangeRate-limit-param' name='limit'/>
		</div>
		<div id='exchangeRate-from-form-field'>
			<label for='from'>from</label>
			<input type='text' id='exchangeRate-from-param' name='from'/>
		</div>
		<div id='exchangeRate-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='exchangeRate-to-param' name='to'/>
		</div>
		<div id='exchangeRate-limit-form-field'>
			<label for='limit'>limit</label>
			<input type='text' id='exchangeRate-limit-param' name='limit'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const fromCurrency = container.querySelector('#exchangeRate-fromCurrency-param');
	const toCurrency = container.querySelector('#exchangeRate-toCurrency-param');
	const from = container.querySelector('#exchangeRate-from-param');
	const to = container.querySelector('#exchangeRate-to-param');
	const limit = container.querySelector('#exchangeRate-limit-param');

	container.querySelector('#exchangeRate-form button').onclick = () => {
		const params = {
			fromCurrency : fromCurrency.value !== "" ? fromCurrency.value : undefined,
			toCurrency : toCurrency.value !== "" ? toCurrency.value : undefined,
			from : from.value !== "" ? from.value : undefined,
			to : to.value !== "" ? to.value : undefined,
			limit : limit.value !== "" ? limit.value : undefined
		};

		exchangeRate(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { exchangeRate, exchangeRateForm };