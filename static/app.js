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
let lastAssistantReply='';
try{chatHistory=JSON.parse(localStorage.getItem('dd_chat_history')||'[]');if(!Array.isArray(chatHistory))chatHistory=[];}catch(_e){chatHistory=[];}
function saveChat(){try{localStorage.setItem('dd_chat_history',JSON.stringify(chatHistory.slice(-40)));}catch(_e){}}
function renderChat(){
  chatMessages.replaceChildren();
  if(!chatHistory.length){const welcome=document.createElement('div');welcome.className='chat-welcome';welcome.textContent='Hello! I’m Data Detective. Ask me anything to get started.';chatMessages.appendChild(welcome);return;}
  chatHistory.forEach(item=>{const bubble=document.createElement('div');bubble.className='chat-bubble '+(item.role==='user'?'chat-user':'chat-ai');bubble.textContent=item.content;chatMessages.appendChild(bubble);});
  chatMessages.scrollTop=chatMessages.scrollHeight;
}
lastAssistantReply=[...chatHistory].reverse().find(item=>item.role==='model')?.content||'';
renderChat();
chatForm.addEventListener('submit',async e=>{
  e.preventDefault();const message=chatInput.value.trim();if(!message)return;
  const previous=chatHistory.slice(-12).map(x=>({role:x.role,content:x.content}));
  chatHistory.push({role:'user',content:message});saveChat();renderChat();chatInput.value='';$('chatSend').disabled=true;
  const pending=document.createElement('div');pending.className='chat-bubble chat-ai chat-pending';pending.textContent='Thinking…';chatMessages.appendChild(pending);chatMessages.scrollTop=chatMessages.scrollHeight;
  try{
    const response=await fetch('/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message,history:previous})});
    const data=await response.json();if(!response.ok)throw new Error(data.error||'Chat request failed');
    chatHistory.push({role:'model',content:data.answer});lastAssistantReply=data.answer;saveChat();renderChat();if(window.dataDetectiveSpeakReply)window.dataDetectiveSpeakReply(data.answer);
  }catch(err){
    chatHistory.pop();saveChat();renderChat();
    const error=document.createElement('div');error.className='chat-bubble chat-error';error.textContent=err.message;chatMessages.appendChild(error);if(window.dataDetectiveVoiceResume)window.dataDetectiveVoiceResume();
  }finally{$('chatSend').disabled=false;chatInput.focus();}
});
$('clearChat').addEventListener('click',()=>{chatHistory=[];lastAssistantReply='';saveChat();renderChat();});


/* Hands-free voice conversation: listen, auto-send, speak, listen again */
(function setupVoiceChat(){
  if(!chatForm || !chatInput || !chatMessages) return;
  const voiceBar=document.createElement('div');
  voiceBar.className='voice-tools';
  voiceBar.innerHTML='<label class="voice-language-label" for="voiceLanguage">زبان</label><select id="voiceLanguage" aria-label="Voice language"><option value="ur-PK">اردو</option><option value="en-US">English</option></select><button type="button" id="voiceMode" class="voice-button">🎙️ گفتگو شروع کریں</button><button type="button" id="speakerMode" class="voice-button voice-toggle" aria-pressed="false">🔊 جواب سنیں</button>';
  chatForm.insertAdjacentElement('afterend',voiceBar);
  const language=$('voiceLanguage'), mode=$('voiceMode'), speaker=$('speakerMode');
  let speakerActive=false;
  const SpeechRecognition=window.SpeechRecognition||window.webkitSpeechRecognition;
  let recognition=null, voiceActive=false, voiceBusy=false, restarting=false;
  function setButton(){
    mode.textContent=voiceActive?'⏹️ گفتگو بند کریں':'🎙️ گفتگو شروع کریں';
    mode.classList.toggle('voice-active',voiceActive);
    mode.setAttribute('aria-pressed',String(voiceActive));
  }
  function startListening(){
    if(!voiceActive||voiceBusy||!recognition||restarting)return;
    try{restarting=true;recognition.lang=language.value;recognition.start();}
    catch(_e){}
    setTimeout(()=>{restarting=false;},500);
  }
  function stopListening(){
    if(recognition){try{recognition.stop();}catch(_e){}}
  }
  function stopSpeaking(){
    if('speechSynthesis' in window) window.speechSynthesis.cancel();
    speakerActive=false;
    if(speaker){speaker.textContent='🔊 جواب سنیں';speaker.setAttribute('aria-pressed','false');}
  }
  function cleanSpeechText(value){
    return String(value||'')
      .replace(/```[\s\S]*?```/g,' ')
      .replace(/`([^`]+)`/g,'$1')
      .replace(/!\[([^\]]*)\]\([^)]+\)/g,'$1')
      .replace(/\[([^\]]+)\]\([^)]+\)/g,'$1')
      .replace(/https?:\/\/\S+/gi,' ')
      .replace(/<[^>]*>/g,' ')
      .replace(/^\s{0,3}#{1,6}\s*/gm,'')
      .replace(/^\s*[-+>]\s+/gm,'')
      .replace(/[*_~|\\/]+/g,' ')
      .replace(/\s+/g,' ')
      .trim();
  }
  function speakText(value){
    const clean=cleanSpeechText(value);
    if(!clean||!('speechSynthesis' in window))return false;
    stopSpeaking();
    const utterance=new SpeechSynthesisUtterance(clean);
    const isUrdu=/[\u0600-\u06FF]/.test(clean);
    utterance.lang=isUrdu?'ur-PK':'en-US';
    const voices=window.speechSynthesis.getVoices();
    const preferred=voices.find(v=>isUrdu?/^ur([_-]|$)/i.test(v.lang):/^en([_-]|$)/i.test(v.lang));
    if(preferred)utterance.voice=preferred;
    utterance.onend=()=>{speakerActive=false;if(speaker){speaker.textContent='🔊 جواب سنیں';speaker.setAttribute('aria-pressed','false');}if(voiceActive){voiceBusy=false;startListening();}};
    utterance.onerror=()=>{speakerActive=false;if(speaker){speaker.textContent='🔊 جواب سنیں';speaker.setAttribute('aria-pressed','false');}if(voiceActive){voiceBusy=false;startListening();}};
    window.speechSynthesis.speak(utterance);
    return true;
  }
  window.dataDetectiveSpeakReply=(answer)=>{
    if(voiceActive){if(!speakText(answer)){voiceBusy=false;startListening();}}
  };
  speaker.addEventListener('click',()=>{
    if(speakerActive){stopSpeaking();return;}
    if(!lastAssistantReply){speaker.textContent='پہلے جواب حاصل کریں';setTimeout(()=>{if(!speakerActive)speaker.textContent='🔊 جواب سنیں';},1600);return;}
    speakerActive=speakText(lastAssistantReply);
    speaker.textContent=speakerActive?'⏹️ آواز بند کریں':'🔊 جواب سنیں';
    speaker.setAttribute('aria-pressed',String(speakerActive));
  });
  window.dataDetectiveVoiceResume=()=>{
    if(voiceActive){voiceBusy=false;startListening();}
  };
  if(!SpeechRecognition){
    mode.disabled=true;
    mode.textContent='اس براؤزر میں وائس دستیاب نہیں';
    mode.title='Android پر Google Chrome استعمال کریں۔';
    return;
  }
  recognition=new SpeechRecognition();
  recognition.interimResults=true;
  recognition.continuous=false;
  recognition.onresult=(event)=>{
    let finalText='';
    for(let i=event.resultIndex;i<event.results.length;i++){
      const part=event.results[i][0].transcript;
      if(event.results[i].isFinal)finalText+=part;
    }
    if(finalText.trim()&&voiceActive&&!voiceBusy){
      chatInput.value=finalText.trim();
      voiceBusy=true;
      stopListening();
      setTimeout(()=>{if(voiceActive)chatForm.requestSubmit();},250);
    }
  };
  recognition.onerror=(event)=>{
    if(event.error==='not-allowed'){
      voiceActive=false;voiceBusy=false;setButton();
      alert('مائیکروفون کی اجازت دیں، پھر وائس گفتگو شروع کریں۔');
    }else if(event.error!=='no-speech'&&event.error!=='aborted'){
      voiceBusy=false;
      if(voiceActive)setTimeout(startListening,700);
    }
  };
  recognition.onend=()=>{
    restarting=false;
    if(voiceActive&&!voiceBusy)setTimeout(startListening,350);
  };
  mode.addEventListener('click',()=>{
    if(voiceActive){
      voiceActive=false;voiceBusy=false;stopListening();stopSpeaking();setButton();
      return;
    }
    voiceActive=true;voiceBusy=false;setButton();startListening();
  });
  setButton();
})();

/* Browser-based PDF creation: no server-side PDF service required. */
(function setupPdfStudio(){
  const button=$('makePdf'), titleField=$('pdfTitle'), bodyField=$('pdfContent');
  if(!button||!titleField||!bodyField)return;
  button.addEventListener('click',()=>{
    const title=titleField.value.trim()||'My document';
    const content=bodyField.value.trim();
    if(!content){alert('Please write or paste some text first.');bodyField.focus();return;}
    const win=window.open('','_blank');
    if(!win){alert('Please allow pop-ups for this website, then try again.');return;}
    const safe=value=>String(value).replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
    win.document.open();
    win.document.write('<!doctype html><html><head><meta charset="utf-8"><title>'+safe(title)+'</title><style>body{font-family:Arial,sans-serif;color:#171717;max-width:780px;margin:48px auto;padding:0 28px;line-height:1.65}h1{font-size:25px;border-bottom:1px solid #ddd;padding-bottom:14px}pre{font:14px/1.7 Arial,sans-serif;white-space:pre-wrap;overflow-wrap:anywhere}@media print{body{margin:18mm auto;padding:0}}</style></head><body><h1>'+safe(title)+'</h1><pre>'+safe(content)+'</pre><script>window.onload=()=>window.print()<\/script></body></html>');
    win.document.close();
  });
})();


/* Private persistent Task Inbox */
(function setupTaskInbox(){
  const form=$('taskForm'), list=$('taskList');
  if(!form||!list)return;
  const csrf=document.querySelector('meta[name="dd-task-csrf"]')?.content||'';
  let tasks=[];
  const message=$('taskMessage');
  function setMessage(text,isError=false){
    message.textContent=text;
    message.classList.toggle('task-message-error',isError);
  }
  function dueLabel(value){
    if(!value)return 'No due date';
    const date=new Date(value);
    if(Number.isNaN(date.getTime()))return 'Due date unavailable';
    return 'Due '+date.toLocaleString([], {dateStyle:'medium',timeStyle:'short'});
  }
  async function api(url,options={}){
    const headers={...(options.headers||{})};
    if(options.body){headers['Content-Type']='application/json';headers['X-CSRF-Token']=csrf;}
    const response=await fetch(url,{...options,headers});
    const data=await response.json().catch(()=>({}));
    if(!response.ok)throw new Error(data.error||'Task request failed.');
    return data;
  }
  function render(){
    list.replaceChildren();
    $('pendingCount').textContent=String(tasks.filter(t=>t.status==='pending').length);
    $('completedCount').textContent=String(tasks.filter(t=>t.status==='completed').length);
    if(!tasks.length){
      const empty=document.createElement('div');empty.className='task-empty';
      const strong=document.createElement('b');strong.textContent='Your inbox is clear';
      const small=document.createElement('span');small.textContent='Add a task on the left and it will appear here.';
      empty.append(strong,small);list.appendChild(empty);return;
    }
    tasks.forEach(task=>{
      const card=document.createElement('article');card.className='task-item'+(task.status==='completed'?' task-done':'');
      const main=document.createElement('div');main.className='task-main';
      const title=document.createElement('b');title.className='task-title';title.textContent=task.title;
      const details=document.createElement('p');details.textContent=task.description||'';
      const due=document.createElement('small');due.className='task-due';due.textContent=dueLabel(task.due_at);
      main.append(title);if(task.description)main.append(details);main.append(due);
      const actions=document.createElement('div');actions.className='task-actions';
      const toggle=document.createElement('button');toggle.type='button';toggle.className='task-action task-complete';
      toggle.textContent=task.status==='completed'?'Reopen':'Complete';
      toggle.addEventListener('click',()=>changeStatus(task,task.status==='completed'?'pending':'completed'));
      const edit=document.createElement('button');edit.type='button';edit.className='task-action';edit.textContent='Edit';
      edit.addEventListener('click',()=>editTask(task));
      const remove=document.createElement('button');remove.type='button';remove.className='task-action task-delete';remove.textContent='Delete';
      remove.addEventListener('click',()=>removeTask(task));
      actions.append(toggle,edit,remove);card.append(main,actions);list.appendChild(card);
    });
  }
  async function loadTasks(){
    setMessage('Loading your private tasks…');
    try{
      const data=await api('/api/tasks');tasks=Array.isArray(data.tasks)?data.tasks:[];
      render();setMessage(tasks.length?'Your tasks are saved securely.':'No tasks yet.');
    }catch(error){
      tasks=[];render();
      if(error.message.includes('Owner login')||error.message.includes('Configure private owner')){
        setMessage('Task Inbox is private. Complete the Owner Login setup in docs/OWNER_AUTH_SETUP.md, then sign in.',true);
      }else setMessage(error.message,true);
    }
  }
  form.addEventListener('submit',async event=>{
    event.preventDefault();
    const title=$('taskTitle').value.trim();
    const description=$('taskDescription').value.trim();
    const localDue=$('taskDue').value;
    if(!title){$('taskTitle').focus();return;}
    const dueAt=localDue?new Date(localDue).toISOString():null;
    $('saveTask').disabled=true;setMessage('Saving your task…');
    try{
      await api('/api/tasks',{method:'POST',body:JSON.stringify({title,description,due_at:dueAt})});
      form.reset();await loadTasks();setMessage('Task saved.');
    }catch(error){setMessage(error.message,true);}
    finally{$('saveTask').disabled=false;}
  });
  async function changeStatus(task,status){
    try{await api('/api/tasks/'+encodeURIComponent(task.id),{method:'PATCH',body:JSON.stringify({status})});await loadTasks();}
    catch(error){setMessage(error.message,true);}
  }
  async function editTask(task){
    const title=prompt('Edit task title:',task.title);
    if(title===null)return;
    const description=prompt('Edit task details:',task.description||'');
    if(description===null)return;
    try{
      await api('/api/tasks/'+encodeURIComponent(task.id),{method:'PATCH',body:JSON.stringify({title,description})});
      await loadTasks();setMessage('Task updated.');
    }catch(error){setMessage(error.message,true);}
  }
  async function removeTask(task){
    if(!confirm('Delete this task permanently?'))return;
    try{await api('/api/tasks/'+encodeURIComponent(task.id),{method:'DELETE',headers:{'X-CSRF-Token':csrf}});await loadTasks();setMessage('Task deleted.');}
    catch(error){setMessage(error.message,true);}
  }
  $('refreshTasks').addEventListener('click',loadTasks);
  loadTasks();
})();


/* Accessible mobile side navigation */
(function setupMobileSidebar(){
  const toggle=$('menuToggle'), sidebar=$('appSidebar'), backdrop=$('sidebarBackdrop');
  if(!toggle||!sidebar||!backdrop)return;
  function setOpen(open){
    document.body.classList.toggle('menu-open',open);
    toggle.setAttribute('aria-expanded',String(open));
    toggle.setAttribute('aria-label',open?'Close navigation menu':'Open navigation menu');
  }
  toggle.addEventListener('click',()=>setOpen(toggle.getAttribute('aria-expanded')!=='true'));
  backdrop.addEventListener('click',()=>setOpen(false));
  sidebar.querySelectorAll('a[href^="#"]').forEach(link=>{
    link.addEventListener('click',()=>setOpen(false));
  });
  document.addEventListener('keydown',event=>{
    if(event.key==='Escape')setOpen(false);
  });
})();


/* Workspace quick search: jump to the best matching tool or section. */
(function setupWorkspaceSearch(){
 const field=$('workspaceSearch'); if(!field)return;
 field.addEventListener('keydown',event=>{
  if(event.key!=='Enter')return;
  const query=field.value.trim().toLowerCase();if(!query)return;
  const candidates=[...document.querySelectorAll('.side-link,.studio-card,.section-heading, .founder-panel')];
  const match=candidates.find(node=>node.textContent.toLowerCase().includes(query));
  if(match){const target=match.closest('section')||match;if(target.id)document.getElementById(target.id)?.scrollIntoView({behavior:'smooth',block:'start'});else target.scrollIntoView({behavior:'smooth',block:'center'});}
  else{field.setCustomValidity('No matching tool found. Try Chat, Image, Video, PDF or Tasks.');field.reportValidity();setTimeout(()=>field.setCustomValidity(''),2200);}
 });
})();


/* Replicate Image & Video Studio */
(function () {
  const byId = (id) => document.getElementById(id);
  const historyKey = "dd-creative-history-v1";
  function setStatus(kind, message) {
    const node = byId(kind + "Status");
    if (node) node.textContent = message;
  }
  function remember(kind, url) {
    try {
      const items = JSON.parse(localStorage.getItem(historyKey) || "[]");
      items.unshift({kind, url, at: new Date().toISOString()});
      localStorage.setItem(historyKey, JSON.stringify(items.slice(0, 12)));
    } catch (_) {}
  }
  function showOutput(kind, output) {
    const box = byId(kind + "Result");
    box.replaceChildren();
    const urls = Array.isArray(output) ? output : [output];
    urls.filter(x => typeof x === "string" && /^https:\/\//i.test(x)).forEach(url => {
      let media;
      if (kind === "video") {
        media = document.createElement("video");
        media.controls = true;
        media.playsInline = true;
        media.src = url;
      } else {
        media = document.createElement("img");
        media.alt = "Generated image";
        media.src = url;
      }
      box.appendChild(media);
      const link = document.createElement("a");
      link.href = url; link.target = "_blank"; link.rel = "noopener";
      link.textContent = "Open generated " + kind;
      box.appendChild(link);
      remember(kind, url);
    });
  }
  async function waitForPrediction(kind, prediction) {
    let current = prediction;
    const id = current.id;
    for (let i = 0; i < 75; i++) {
      if (current.status === "succeeded") {
        showOutput(kind, current.output);
        setStatus(kind, "Completed. Your " + kind + " is ready.");
        return;
      }
      if (["failed", "canceled"].includes(current.status)) {
        throw new Error(current.error || ("Generation " + current.status + "."));
      }
      setStatus(kind, "Generating… " + (current.status || "starting") + " (" + (i + 1) + "/75)");
      await new Promise(resolve => setTimeout(resolve, 4000));
      const response = await fetch("/api/creative/status/" + encodeURIComponent(id));
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "Could not check generation status.");
      current = data.prediction;
    }
    setStatus(kind, "Still processing. You can check again later; this request may continue on Replicate.");
  }
  async function generate(kind) {
    const isImage = kind === "image";
    const button = byId(isImage ? "generateImage" : "generateVideo");
    const prompt = byId(isImage ? "imagePrompt" : "videoPrompt").value.trim();
    if (!prompt) { setStatus(kind, "Please write a description first."); return; }
    const form = new FormData();
    form.append("prompt", prompt);
    const fileInput = byId(isImage ? "imageReference" : "videoReference");
    if (fileInput.files[0]) form.append("reference", fileInput.files[0]);
    if (isImage) form.append("aspect_ratio", byId("imageRatio").value);
    button.disabled = true;
    setStatus(kind, "Sending your request securely to Replicate…");
    byId(kind + "Result").replaceChildren();
    try {
      const response = await fetch("/api/creative/" + kind, {method: "POST", body: form});
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "Generation request failed.");
      await waitForPrediction(kind, data.prediction);
    } catch (error) {
      setStatus(kind, error.message || "Something went wrong. Please try again.");
    } finally {
      button.disabled = false;
    }
  }
  byId("generateImage")?.addEventListener("click", () => generate("image"));
  byId("generateVideo")?.addEventListener("click", () => generate("video"));
  fetch("/api/creative/config").then(r => r.json()).then(config => {
    setStatus("image", config.configured ? "Image service connected. Generation may use paid credits after any free trial." : "Replicate token not detected by this server yet.");
    setStatus("video", config.configured ? "Video service connected. Video generation may cost more credits." : "Replicate token not detected by this server yet.");
  }).catch(() => {
    setStatus("image", "Could not check Replicate connection.");
    setStatus("video", "Could not check Replicate connection.");
  });
})();
