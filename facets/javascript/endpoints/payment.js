const payment = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/payment/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			0 : parameters.0
		})
	});
}

const paymentForm = (container) => {
	const html = `<form id='payment-form'>
		<div id='payment-0-form-field'>
			<label for='0'>0</label>
			<input type='text' id='payment-0-param' name='0'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const 0 = container.querySelector('#payment-0-param');

	container.querySelector('#payment-form button').onclick = () => {
		const params = {
			0 : 0.value !== "" ? 0.value : undefined
		};

		payment(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { payment, paymentForm };