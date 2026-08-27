# **Documento de Critérios de Aceitação — Sistema de Classificação de Incidentes (HU)**

Este documento define os critérios de aceitação para as principais funcionalidades do Sistema de Classificação de Incidentes do Hospital Universitário (HU), baseado nos requisitos e atas de alinhamento estabelecidos.  
 

## **Item 1 — Abertura de Notificação via QR Code**

### **Funcionalidade**

*“Como funcionário, eu quero acessar o formulário através de um QR Code pelo celular, para relatar um erro rapidamente sem comprometer o tempo do meu turno.”*

### **Critérios de aceitação**

\[ \] O sistema deve direcionar o usuário para o tipo de formulário correto dependendo do QR Code que ele leu, contando com 1 nível de acesso para funcionários e 1 acesso para pacientes/acompanhantes.  
\[ \] A interface deve ser desenvolvida com foco em dispositivos móveis (Mobile First), garantindo navegação intuitiva para preenchimento rápido via celular.  
\[ \] O tempo de resposta para o carregamento do formulário e envio da notificação não deve ultrapassar 5 segundos.  
\[ \] O formulário deve conter campos obrigatórios para dados de pré-classificação do incidente, descrição do que aconteceu e o número do prontuário do paciente (quando for o caso).  
\[ \] O sistema deve oferecer a opção de anexar arquivos à notificação, como fotos e documentos.  
 

## **Item 2 — Anonimato e Identificação Opcional**

### **Funcionalidade**

*“Como paciente ou acompanhante, eu quero preencher a notificação anonimamente, para não ter medo de sofrer retaliações ao expor um problem.”*

### **Critérios de aceitação**

\[ \] O sistema deve garantir que a notificação possa ser enviada de forma totalmente anônima.  
\[ \] O sistema deve disponibilizar um campo opcional para inserção de e-mail, caso o usuário deseje receber um retorno do hospital sobre a resolução.  
\[ \] O sistema deve assegurar o anonimato e a segurança dos dados.  
 

## **Item 3 — Triagem e Gestão de Incidentes**

### **Funcionalidade**

*“Como avaliador do NSP, eu quero visualizar as novas notificações em um único painel, para não perder tempo cruzando dados entre várias planilhas de Excel.”*

### **Critérios de aceitação**

\[ \] O sistema deve permitir a visualização centralizada de todas as notificações em um único painel.  
\[ \] O painel deve fornecer uma opção para expandir as informações de cada notificação antes do início formal da triagem, facilitando a visualização prévia.  
\[ \] O sistema deve exibir o número do prontuário e a gravidade de imediato para permitir a comparação de dados e identificação de notificações duplicadas.  
\[ \] O sistema deve permitir que o analista altere ou reclassifique o incidente durante a análise técnica.  
\[ \] O painel deve oferecer ferramentas de pesquisa e filtros por data, prontuário ou gravidade.  
 

## **Item 4 — Investigação e Plano de Ação**

### **Funcionalidade**

*“Como membro da comissão, eu quero ter campos para montar o plano de ação (5W2H) direto no sistema, para definir o que precisa ser feito e evitar que o erro aconteça de novo.”*

### **Critérios de aceitação**

\[ \] O sistema deve disponibilizar campos estruturados utilizando a metodologia 5W2H para a definição das medidas corretivas.  
\[ \] O sistema deve permitir o registro e o acompanhamento cronológico da investigação do incidente, salvando datas e causas identificadas.  
\[ \] O sistema deve obrigar o registro da causa raiz do incidente ao finalizar a investigação.  
\[ \] O status da notificação deve ser atualizado de acordo com o progresso, seguindo o fluxo: Aberto, Em Análise, Investigação da Causa Raiz, Plano de Ação e Concluído.  
\[ \] O controle de acesso a esses dados sensíveis deve ser restrito por níveis, permitindo a visualização apenas a funcionários autorizados do setor de Qualidade e NSP.  
 

## **Item 5 — Coleta de Indicadores e Relatórios (BI) \- Opcional**

### **Funcionalidade**

*“Como membro da comissão, eu quero que o sistema exporte esses dados para ferramentas de BI, para a gente conseguir gerar os gráficos e acompanhar se os incidentes estão sendo resolvidos.”*

### **Critérios de aceitação**

\[ \] O sistema deve permitir o preenchimento manual dos formulários de indicadores pelos usuários internos.  
\[ \] Os formulários de indicadores devem permitir o salvamento intermediário e a reedição dos dados, dividindo a coleta em duas etapas distintas.  
\[ \] O sistema deve permitir a exportação de dados consolidados diretamente para alimentar as planilhas de indicadores do hospital.  
\[ \] O sistema deve organizar e estruturar as informações coletadas para facilitar a integração nativa com ferramentas de BI.