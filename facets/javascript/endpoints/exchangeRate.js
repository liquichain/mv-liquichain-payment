const exchangeRate = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/exchangeRate/${parameters.fromCurrency}/${parameters.toCurrency}`, baseUrl);
	if (parameters.from !== undefined) {
		url.searchParams.append('from', parameters.from);
	}

	if (parameters.to !== undefined) {
		url.searchParams.append('to', parameters.to);
	}

	if (parameters.from !== undefined) {
		url.searchParams.append('from', parameters.from);
	}

	if (parameters.to !== undefined) {
		url.searchParams.append('to', parameters.to);
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
		<div id='exchangeRate-null-form-field'>
			<label for='null'>null</label>
			<input type='text' id='exchangeRate-null-param' name='null'/>
		</div>
		<div id='exchangeRate-null-form-field'>
			<label for='null'>null</label>
			<input type='text' id='exchangeRate-null-param' name='null'/>
		</div>
		<div id='exchangeRate-null-form-field'>
			<label for='null'>null</label>
			<input type='text' id='exchangeRate-null-param' name='null'/>
		</div>
		<div id='exchangeRate-null-form-field'>
			<label for='null'>null</label>
			<input type='text' id='exchangeRate-null-param' name='null'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const fromCurrency = container.querySelector('#exchangeRate-fromCurrency-param');
	const toCurrency = container.querySelector('#exchangeRate-toCurrency-param');
	const null = container.querySelector('#exchangeRate-null-param');
	const null = container.querySelector('#exchangeRate-null-param');
	const null = container.querySelector('#exchangeRate-null-param');
	const null = container.querySelector('#exchangeRate-null-param');

	container.querySelector('#exchangeRate-form button').onclick = () => {
		const params = {
			fromCurrency : fromCurrency.value !== "" ? fromCurrency.value : undefined,
			toCurrency : toCurrency.value !== "" ? toCurrency.value : undefined,
			null : null.value !== "" ? null.value : undefined,
			null : null.value !== "" ? null.value : undefined,
			null : null.value !== "" ? null.value : undefined,
			null : null.value !== "" ? null.value : undefined
		};

		exchangeRate(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { exchangeRate, exchangeRateForm };