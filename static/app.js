const $=id=>document.getElementById(id);let sessionId=null;const fileInput=$('file'),drop=$('drop');
fileInput.addEventListener('change',()=>{$('filename').textContent=fileInput.files[0]?.name||'No file selected'});
['dragenter','dragover'].forEach(e=>drop.addEventListener(e,x=>{x.preventDefault();drop.style.borderColor='#72e0bd'}));['dragleave','drop'].forEach(e=>drop.addEventListener(e,x=>{x.preventDefault();drop.style.borderColor=''}));drop.addEventListener('drop',e=>{const f=e.dataTransfer.files[0];if(f&&f.name.toLowerCase().endsWith('.csv')){const dt=new DataTransfer();dt.items.add(f);fileInput.files=dt.files;$('filename').textContent=f.name}});
function progress(title,detail){$('progressTitle').textContent=title;$('progressText').textContent=detail;$('progress').classList.remove('hidden');$('empty').classList.add('hidden');$('status').textContent='Working'}
function renderChart(chart){
const panel=$('chartPanel'),bars=$('chartBars');
bars.replaceChildren();
if(!chart||!Array.isArray(chart.labels)||!chart.labels.length){panel.classList.add('hidden');return}
$('chartTitle').textContent=chart.title||'Data chart';
$('chartMeta').textContent=chart.metric+' · '+chart.category;
const max=Math.max(...chart.values.map(Number),0);
chart.labels.forEach((label,i)=>{
const value=Number(chart.values[i])||0;
const row=document.createElement('div');row.className='chart-row';
const name=document.createElement('div');name.className='chart-label';name.textContent=label;
const track=document.createElement('div');track.className='chart-track';
const fill=document.createElement('div');fill.className='chart-fill';fill.style.width=(max>0?Math.max(2,value/max*100):0)+'%';
track.appendChild(fill);
const amount=document.createElement('div');amount.className='chart-value';amount.textContent=Number.isInteger(value)?String(value):value.toLocaleString(undefined,{maximumFractionDigits:2});
row.append(name,track,amount);bars.appendChild(row);
});
panel.classList.remove('hidden');
}
function showAnswer(text){$('answer').textContent=text;$('answer').classList.remove('hidden');$('empty').classList.add('hidden');$('status').textContent='Complete';$('progress').classList.add('hidden');$('reportLink').classList.remove('hidden');$('followupBox').classList.remove('hidden')}
$('analyze').addEventListener('click',async()=>{const f=fileInput.files[0];if(!f){alert('Please choose a CSV file first.');return}const fd=new FormData();fd.append('file',f);fd.append('question',$('question').value);$('analyze').disabled=true;progress('Analyzing your CSV…','Running the free Python analysis engine');try{const res=await fetch('/api/analyze',{method:'POST',body:fd});const data=await res.json();if(!res.ok)throw new Error(data.error||'Analysis failed');sessionId=data.id;$('downloadReport').href=data.report_url;showAnswer(data.answer);renderChart(data.chart);$('status').textContent='Analysis complete'}catch(e){$('status').textContent='Error';$('progress').classList.add('hidden');$('empty').classList.remove('hidden');$('empty').innerHTML='<b>We could not complete the analysis</b><span>'+escapeHtml(e.message)+'</span>'}finally{$('analyze').disabled=false}});
$('sendFollowup').addEventListener('click',async()=>{const q=$('followup').value.trim();if(!q||!sessionId)return;$('sendFollowup').disabled=true;progress('Checking your follow-up question…','Using the same uploaded dataset');try{const r=await fetch('/api/followup/'+sessionId,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({question:q})});const d=await r.json();if(!r.ok)throw new Error(d.error||'Follow-up failed');$('downloadReport').href=d.report_url;showAnswer($('answer').textContent+'\n\nYOU: '+q+'\n\nDATA DETECTIVE: '+d.answer);renderChart(d.chart);$('followup').value=''}catch(e){$('progressText').textContent=e.message;$('progressTitle').textContent='Could not continue'}finally{$('sendFollowup').disabled=false}});
function escapeHtml(s){return String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}

/* General Gemini chat */
const chatForm=$('chatForm'),chatInput=$('chatInput'),chatMessages=$('chatMessages');
let chatHistory=[];
try{chatHistory=JSON.parse(localStorage.getItem('dd_chat_history')||'[]');if(!Array.isArray(chatHistory))chatHistory=[];}catch(_e){chatHistory=[];}
function saveChat(){try{localStorage.setItem('dd_chat_history',JSON.stringify(chatHistory.slice(-40)));}catch(_e){}}
function renderChat(){
  chatMessages.replaceChildren();
  if(!chatHistory.length){const welcome=document.createElement('div');welcome.className='chat-welcome';welcome.textContent='Hello! I’m Data Detective. Ask me anything to get started.';chatMessages.appendChild(welcome);return;}
  chatHistory.forEach(item=>{const bubble=document.createElement('div');bubble.className='chat-bubble '+(item.role==='user'?'chat-user':'chat-ai');bubble.textContent=item.content;chatMessages.appendChild(bubble);});
  chatMessages.scrollTop=chatMessages.scrollHeight;
}
renderChat();
chatForm.addEventListener('submit',async e=>{
  e.preventDefault();const message=chatInput.value.trim();if(!message)return;
  const previous=chatHistory.slice(-12).map(x=>({role:x.role,content:x.content}));
  chatHistory.push({role:'user',content:message});saveChat();renderChat();chatInput.value='';$('chatSend').disabled=true;
  const pending=document.createElement('div');pending.className='chat-bubble chat-ai chat-pending';pending.textContent='Thinking…';chatMessages.appendChild(pending);chatMessages.scrollTop=chatMessages.scrollHeight;
  try{
    const response=await fetch('/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message,history:previous})});
    const data=await response.json();if(!response.ok)throw new Error(data.error||'Chat request failed');
    chatHistory.push({role:'model',content:data.answer});saveChat();renderChat();
  }catch(err){
    chatHistory.pop();saveChat();renderChat();
    const error=document.createElement('div');error.className='chat-bubble chat-error';error.textContent=err.message;chatMessages.appendChild(error);
  }finally{$('chatSend').disabled=false;chatInput.focus();}
});
$('clearChat').addEventListener('click',()=>{chatHistory=[];saveChat();renderChat();});
