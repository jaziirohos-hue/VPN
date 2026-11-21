const {chromium}=require('playwright');
(async()=>{const b=await chromium.launch();const p=await b.newPage();
await p.goto('file://'+process.cwd()+'/form.html',{waitUntil:'networkidle'});
await p.pdf({path:'JSCH-Medicine-Form.pdf',format:'A4',printBackground:true,preferCSSPageSize:true});
await p.setViewportSize({width:794,height:1123});await p.screenshot({path:'/tmp/claude-0/-home-user-VPN/6fba01ac-3a73-5d35-86a7-1942421f614f/scratchpad/preview.png',fullPage:true});
await b.close();})();
