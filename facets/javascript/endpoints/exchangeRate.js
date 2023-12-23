const exchangeRate = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/exchangeRate/${parameters.toCurrency}`, baseUrl);
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
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const toCurrency = container.querySelector('#exchangeRate-toCurrency-param');
	const from = container.querySelector('#exchangeRate-from-param');
	const to = container.querySelector('#exchangeRate-to-param');

	container.querySelector('#exchangeRate-form button').onclick = () => {
		const params = {
			toCurrency : toCurrency.value !== "" ? toCurrency.value : undefined,
			from : from.value !== "" ? from.value : undefined,
			to : to.value !== "" ? to.value : undefined
		};

		exchangeRate(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { exchangeRate, exchangeRateForm };