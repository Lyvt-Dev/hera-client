import {access} from 'node:fs/promises';
for(const file of ['index.html','style.css','app.js','auth.js','fonts/nunito-variable.ttf','fonts/OFL-Nunito.txt']) await access('dist/'+file);
console.log('Static build ready in dist/');
