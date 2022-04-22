const currconv = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/currconv/`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const currconvForm = (container) => {
	const html = `<form id='currconv-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#currconv-form button').onclick = () => {
		const params = {

		};

		currconv(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { currconv, currconvForm };