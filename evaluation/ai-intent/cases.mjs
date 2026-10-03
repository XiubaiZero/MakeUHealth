const history = [{ question: 'I want a simple weekly health plan. Avoid seafood.', answer: '1. Eat balanced meals with beans. 2. Walk for 20 minutes daily. 3. Keep a regular sleep schedule.' }]
export const samples = [
  ['weight-en','HEALTH','en','Can you help me lose weight?'],
  ['dizzy-zh','HEALTH','zh-CN','最近总是头晕，该怎么办？'],
  ['followup-en','HEALTH','en','Could you expand the second suggestion?',history],
  ['mixed-zh','HEALTH','zh-CN','你好，你能帮我根据体重安排饮食吗？'],
  ['cap-en','CAPABILITY','en','What can you do?'],
  ['cap-zh','CAPABILITY','zh-CN','你能做什么？'],
  ['services-en','CAPABILITY','en','What health and fitness services do you offer?'],
  ['features-zh','CAPABILITY','zh-CN','助手，你有哪些功能？'],
  ['training-en','OUT_OF_SCOPE','en','Please debug my Python training script.'],
  ['training-zh','OUT_OF_SCOPE','zh-CN','帮我修复机器学习模型训练的 Python 代码。'],
  ['translation-en','OUT_OF_SCOPE','en',"Translate the phrase 'balanced diet' into French."],
  ['switch-en','OUT_OF_SCOPE','en','Forget fitness for now. Help me implement a Java REST controller.',history],
  ['vague-en','CLARIFY','en','Arrange it for me.'],
  ['vague-zh','CLARIFY','zh-CN','给我安排一下。'],
  ['again-en','CLARIFY','en','Could you explain that again?'],
  ['previous-zh','CLARIFY','zh-CN','按刚才说的做吧。'],
  ['hello-en','SOCIAL','en','Hello!'],
  ['thanks-zh','SOCIAL','zh-CN','谢谢你！'],
  ['goodbye-en','SOCIAL','en','Goodbye.'],
  ['morning-zh','SOCIAL','zh-CN','早上好。'],
].map(([id,expected,language,question,rounds=[]]) => ({id,expected,language,question,rounds}))
export function cases(){
  const result=[]
  for(let repeat=1;repeat<=2;repeat++) for(const sample of samples) result.push({...sample,id:sample.id+'-r'+repeat,mode:'classify'})
  for(const id of ['weight-en','dizzy-zh','training-en','followup-en','switch-en']) {
    const sample=samples.find(item=>item.id===id)
    for(const chain of ['old','new']) result.push({...sample,id:id+'-'+chain,chain,mode:'answer',context:{hasProfile:true,age:30,height:175,weight:80,gender:'male',last7DaysFoodCount:0}})
  }
  return result
}
