import express from 'express';
import session from 'express-session';
import multer from 'multer';
import AdmZip from 'adm-zip';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'..');
const dataDir=process.env.PXG_DATA_DIR||path.join(root,'data'); const uploadDir=path.join(dataDir,'uploads');
fs.mkdirSync(uploadDir,{recursive:true});
const dbPath=path.join(dataDir,'db.json');
const now=Date.now();
const initial={
  projects:[
    {id:'p-starter',name:'Getting started',description:'A safe place to inspect, organize, and understand files.',color:'#7856e8',createdAt:new Date(now-86400000*9).toISOString()},
    {id:'p-mods',name:'Game mod toolkit',description:'Review community mod archives and source files.',color:'#15a889',createdAt:new Date(now-86400000*5).toISOString()},
    {id:'p-lab',name:'Code review lab',description:'Static code reading and language notes.',color:'#e6a23c',createdAt:new Date(now-86400000*2).toISOString()}
  ], files:[
    {id:'f-demo-1',name:'README.md',size:1840,type:'Source & text',extension:'.md',mime:'text/markdown',hash:'9f4c2a17e3b61d8c4a927e1f6b39d81a7c6e52f0d81b4a39e7206c5f13a98b44',language:'Markdown',preview:'# Lunar client setup\n\nA small mod-pack workspace for experimenting with Java-based game mods.\n\n## Getting started\n- Keep a backup of your worlds.\n- Review each archive before installing.\n- Use trusted sources only.\n',entries:[],projectId:'p-mods',createdAt:new Date(now-3600000*2).toISOString(),url:null,safety:'Ready to inspect'},
    {id:'f-demo-2',name:'ModConfig.java',size:2634,type:'Source & text',extension:'.java',mime:'text/x-java',hash:'8c10be5f6136aa7a94321cc4ab7d806ef1a2bb16e5f4c743f9751f0a2df8a941',language:'Java',preview:'package dev.lunar.config;\n\npublic final class ModConfig {\n  public static final String MOD_ID = "lunar-tools";\n  public static final int DEFAULT_RANGE = 12;\n\n  private ModConfig() { }\n\n  public static boolean isRangeValid(int value) {\n    return value >= 1 && value <= 64;\n  }\n}\n',entries:[],projectId:'p-mods',createdAt:new Date(now-3600000*5).toISOString(),url:null,safety:'Ready to inspect'},
    {id:'f-demo-3',name:'lunar-modpack.zip',size:2489132,type:'Archive',extension:'.zip',mime:'application/zip',hash:'7d8bb2e7a879f804c92676ec5aa5c733',language:'',preview:null,entries:[{name:'config/',directory:true,size:0},{name:'config/lunar-tools.json',directory:false,size:842},{name:'mods/README.md',directory:false,size:1840},{name:'resourcepacks/moonlight.zip',directory:false,size:2184300}],projectId:'p-mods',createdAt:new Date(now-3600000*22).toISOString(),url:null,safety:'Archive listing ready'}
  ], notes:[
    {id:'n-1',title:'Safe file handling checklist',body:'Inspect before opening. Archives can contain nested files. Never run unknown executables.',projectId:'p-starter',createdAt:new Date(now-86400000).toISOString()}
  ], activity:[
    {id:'a-demo-1',action:'Inspected archive',detail:'lunar-modpack.zip',createdAt:new Date(now-3600000*22).toISOString()},
    {id:'a-demo-2',action:'Added source file',detail:'ModConfig.java',createdAt:new Date(now-3600000*5).toISOString()},
    {id:'a-demo-3',action:'Added note',detail:'Safe file handling checklist',createdAt:new Date(now-86400000).toISOString()}
  ]
};
let db;
try{ db=JSON.parse(fs.readFileSync(dbPath,'utf8')); }catch{db=initial;save();}
function save(){fs.writeFileSync(dbPath,JSON.stringify(db,null,2));}
const app=express(); app.set('trust proxy',1); app.use(express.json({limit:'2mb'}));
app.use(session({secret:process.env.SESSION_SECRET||crypto.randomBytes(32).toString('hex'),resave:false,saveUninitialized:false,cookie:{httpOnly:true,sameSite:'lax',secure:false,maxAge:7*86400000}}));
const auth=(req,res,next)=>req.session.user?next():res.status(401).json({error:'Please sign in to continue.'});
app.get('/uploads/:filename',auth,(req,res)=>{const filename=path.basename(req.params.filename);const record=db.files.find(f=>f.url===`/uploads/${filename}`);if(!record)return res.sendStatus(404);const target=path.join(uploadDir,filename);if(!fs.existsSync(target))return res.sendStatus(404);res.download(target,record.name);});
const addActivity=(action,detail)=>{db.activity.unshift({id:crypto.randomUUID(),action,detail,createdAt:new Date().toISOString()});db.activity=db.activity.slice(0,40);save();};
app.post('/api/auth/login',(req,res)=>{const {email,password}=req.body||{};if(email!=='demo@pxg.dev'||password!=='demo1234')return res.status(401).json({error:'That email and password did not match.'});req.session.user={id:'user-demo',name:'Alex Morgan',email:'demo@pxg.dev'};res.json({user:req.session.user});});
app.get('/api/auth/me',(req,res)=>res.json({user:req.session.user||null}));
app.post('/api/auth/logout',(req,res)=>req.session.destroy(()=>res.json({ok:true})));
app.get('/api/dashboard',auth,(req,res)=>res.json({projects:db.projects,files:db.files,notes:db.notes,activity:db.activity}));
app.post('/api/projects',auth,(req,res)=>{const name=String(req.body.name||'').trim();if(!name)return res.status(400).json({error:'Project name is required.'});const p={id:crypto.randomUUID(),name:name.slice(0,80),description:String(req.body.description||'').slice(0,240),color:req.body.color||'#7856e8',createdAt:new Date().toISOString()};db.projects.unshift(p);save();addActivity('Created project',p.name);res.status(201).json(p);});
app.patch('/api/projects/:id',auth,(req,res)=>{const p=db.projects.find(x=>x.id===req.params.id);if(!p)return res.sendStatus(404);if(req.body.name)p.name=String(req.body.name).slice(0,80);if(req.body.description!==undefined)p.description=String(req.body.description).slice(0,240);if(req.body.color)p.color=String(req.body.color).slice(0,16);save();res.json(p);});
app.delete('/api/projects/:id',auth,(req,res)=>{const [p]=db.projects.filter(x=>x.id===req.params.id);if(!p)return res.sendStatus(404);db.projects=db.projects.filter(x=>x.id!==p.id);db.notes=db.notes.filter(x=>x.projectId!==p.id);db.files=db.files.map(f=>f.projectId===p.id?{...f,projectId:null}:f);save();addActivity('Deleted project',p.name);res.json({ok:true});});
const storage=multer.diskStorage({destination:uploadDir,filename:(req,file,cb)=>cb(null,crypto.randomUUID()+path.extname(file.originalname).slice(0,16))});
const upload=multer({storage,limits:{fileSize:100*1024*1024,files:15}});
function inspectFile(file){const ext=path.extname(file.originalname).toLowerCase();const bytes=fs.readFileSync(file.path);const hash=crypto.createHash('sha256').update(bytes).digest('hex');const mime=file.mimetype||'application/octet-stream';const textLike=/\.(txt|md|json|js|jsx|ts|tsx|py|java|c|cpp|h|hpp|cs|go|rs|php|rb|html|css|xml|yaml|yml|toml|sh|bat|sql|log|ini|dart|kt|swift|vue|svelte)$/i.test(file.originalname);let preview=null,entries=[];
 if(textLike&&!bytes.subarray(0,4096).includes(0)){preview=bytes.toString('utf8').slice(0,18000);}
 if(ext==='.zip'){try{const zip=new AdmZip(file.path);entries=zip.getEntries().slice(0,200).map(e=>({name:e.entryName,directory:e.isDirectory,size:e.header.size}));}catch{entries=[];}}
 const category=ext==='.zip'?'Archive':ext==='.rar'?'Archive':['.exe','.dll','.msi','.msix','.jar','.app','.com','.scr','.apk'].includes(ext)?'Executable':textLike?'Source & text':ext==='.exr'?'Image':'Other';
 let language='';const langMap={'.py':'Python','.java':'Java','.js':'JavaScript','.jsx':'React JSX','.ts':'TypeScript','.tsx':'React TSX','.dart':'Dart','.rs':'Rust','.go':'Go','.c':'C','.cpp':'C++','.cs':'C#','.php':'PHP','.rb':'Ruby','.html':'HTML','.css':'CSS','.json':'JSON','.md':'Markdown','.sh':'Shell'};language=langMap[ext]||'';
 return {id:crypto.randomUUID(),name:path.basename(file.originalname).slice(0,200),size:file.size,type:category,extension:ext||'file',mime,hash,language,preview,entries,projectId:null,createdAt:new Date().toISOString(),url:'/uploads/'+path.basename(file.path),safety:category==='Executable'?'Static inspection only':'Ready to inspect'};
}
app.post('/api/files',auth,upload.array('files',15),(req,res)=>{try{const added=(req.files||[]).map(inspectFile);db.files.unshift(...added);save();added.forEach(f=>addActivity('Added file',f.name));res.status(201).json(added);}catch(e){res.status(400).json({error:'Could not inspect that file.'});}});
app.patch('/api/files/:id',auth,(req,res)=>{const f=db.files.find(x=>x.id===req.params.id);if(!f)return res.sendStatus(404);if(req.body.projectId!==undefined)f.projectId=req.body.projectId||null;if(req.body.name)f.name=String(req.body.name).slice(0,200);save();res.json(f);});
app.delete('/api/files/:id',auth,(req,res)=>{const f=db.files.find(x=>x.id===req.params.id);if(!f)return res.sendStatus(404);db.files=db.files.filter(x=>x.id!==f.id);const filePath=path.join(uploadDir,path.basename(f.url));if(fs.existsSync(filePath))fs.unlinkSync(filePath);save();addActivity('Removed file',f.name);res.json({ok:true});});
app.post('/api/notes',auth,(req,res)=>{const title=String(req.body.title||'').trim();const body=String(req.body.body||'').trim();if(!title||!body)return res.status(400).json({error:'A title and note are required.'});const n={id:crypto.randomUUID(),title:title.slice(0,100),body:body.slice(0,5000),projectId:req.body.projectId||null,createdAt:new Date().toISOString()};db.notes.unshift(n);save();addActivity('Created note',n.title);res.status(201).json(n);});
app.patch('/api/notes/:id',auth,(req,res)=>{const n=db.notes.find(x=>x.id===req.params.id);if(!n)return res.sendStatus(404);for(const key of ['title','body','projectId'])if(req.body[key]!==undefined)n[key]=req.body[key];save();res.json(n);});
app.delete('/api/notes/:id',auth,(req,res)=>{const n=db.notes.find(x=>x.id===req.params.id);if(!n)return res.sendStatus(404);db.notes=db.notes.filter(x=>x.id!==n.id);save();addActivity('Deleted note',n.title);res.json({ok:true});});
app.get('/api/health',(_,res)=>res.json({ok:true}));
app.get('/download/pxg-file-workbench-source.zip',(req,res)=>res.download(path.join(root,'release','pxg-file-workbench-source.zip')));
const dist=path.join(root,'dist');if(fs.existsSync(dist)){app.use(express.static(dist));app.get('*',(req,res)=>res.sendFile(path.join(dist,'index.html')));}
app.use((err,req,res,next)=>{console.error(err);res.status(err.code==='LIMIT_FILE_SIZE'?413:400).json({error:err.code==='LIMIT_FILE_SIZE'?'Files must be under 100 MB.':'Upload could not be processed.'});});
const port=process.env.PORT||4100;app.listen(port,process.env.PXG_HOST||'0.0.0.0',()=>console.log(`PXG API ready on ${port}`));
