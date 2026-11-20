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
    chatHistory.push({role:'model',content:data.answer});lastAssistantReply=data.answer;saveChat();renderChat();if(window.dataDetectiveSpeakReply)window.dataDetectiveSpeakReply(data.answer);
  }catch(err){
    chatHistory.pop();saveChat();renderChat();
    const error=document.createElement('div');error.className='chat-bubble chat-error';error.textContent=err.message;chatMessages.appendChild(error);if(window.dataDetectiveVoiceResume)window.dataDetectiveVoiceResume();
  }finally{$('chatSend').disabled=false;chatInput.focus();}
});
$('clearChat').addEventListener('click',()=>{chatHistory=[];saveChat();renderChat();});


/* Hands-free voice conversation: listen, auto-send, speak, listen again */
(function setupVoiceChat(){
  if(!chatForm || !chatInput || !chatMessages) return;
  const voiceBar=document.createElement('div');
  voiceBar.className='voice-tools';
  voiceBar.innerHTML='<label class="voice-language-label" for="voiceLanguage">زبان</label><select id="voiceLanguage" aria-label="Voice language"><option value="ur-PK">اردو</option><option value="en-US">English</option></select><button type="button" id="voiceMode" class="voice-button">🎙️ گفتگو شروع کریں</button><button type="button" id="speakerMode" class="voice-button voice-toggle" aria-pressed="false">🔊 جواب سنیں</button>';
  chatForm.insertAdjacentElement('afterend',voiceBar);
  const language=$('voiceLanguage'), mode=$('voiceMode'), speaker=$('speakerMode');
  let lastAssistantReply='';
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
