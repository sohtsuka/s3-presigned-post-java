async function upload(file) {
  let response = await fetch('/signed-post');
  let json = await response.json();
  console.log('json:', json);

  let form = new FormData();
  Object.keys(json.fields).forEach(key => form.append(key, json.fields[key]));
  form.append('file', file);

  response = await fetch(json.url, { method: 'POST', body: form });
  console.log('response:', response);
  if (!response.ok) return 'アップロード失敗';

  return `アップロード成功: key=${json.fields['key']}`;
}

function init() {
  const fileinput = document.getElementById('fileinput');
  const uploadbutton = document.getElementById('uploadbutton');
  const output = document.getElementById('output');

  uploadbutton.addEventListener('click', async () => {
    const file = fileinput.files[0];
    const result = await upload(file);
    output.innerText = result;
  });
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init);
} else {
  init();
}
