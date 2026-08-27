Centro de Tecnologia \- CTC   
Departamento de Informática \- DIN   
Ciência da Computação   
Disciplina: Laboratório de Computação Aplicada \- 12038   
Discentes: Dacio Fernando Machado Francisco

# **Sistema de Classificação de Incidentes do HU \- Análise de Requisitos**

Discentes:   
Fernando Brito Campideli \- R.A.: 134679  
Igor Töebe Lopes Farias \- R.A.: 120173  
Gustavo Moretto Itikawa \- R.A.: 90416  
Gabriel Paeslandin C. \- R.A.: 117208  
Guilherme Lazaro N. \- R.A.: 114016

Maringá, 2025

## **Compilação da Avaliação Periódica 1**

### **1\. Briefing do Projeto**

Atualmente, o Hospital Universitário (HU) conta com um processo totalmente manual para o atendimento de notificações de pacientes. Os pacientes utilizam QR Codes espalhados em cartazes pelo hospital para acessar formulários via Google Forms. Os dados gerados são recolhidos pelos funcionários, estruturados em planilhas do Excel e, a partir daí, o tratamento dos incidentes é realizado.

O objetivo deste projeto é desenvolver um sistema automatizado capaz de receber os formulários preenchidos e encaminhá-los diretamente para o tratamento ágil pela equipe hospitalar responsável. Com essa solução, espera-se eliminar o trabalho moroso de orquestração manual de dados em múltiplas planilhas e impulsionar a taxa de resolução das notificações pendentes. O sistema deve aderir às normativas da ANVISA e do programa Hospital Sentinela.

### **2\. Lista de Funcionalidades**

As funcionalidades foram reorganizadas com base no nível de prioridade estabelecido pela equipe:

**Alta Prioridade (Foco no Paciente/Notificante)**

* Acesso prioritário e direcionamento de formulários através da leitura de QR Codes (1 acesso para pacientes/acompanhantes e 1 para funcionários).  
* Preenchimento de notificação com garantia de anonimato absoluto.  
* Disponibilização de campo opcional para inserção de e-mail, focado em pacientes ou funcionários que desejam receber feedback sobre a resolução.  
* Opção para anexo de evidências digitais (documentos e fotos) no momento da notificação.

**Prioridade Média (Fluxo Interno de Qualidade e NSP)**

* Painel centralizado para que a equipe de Qualidade/NSP visualize as notificações.  
* Detecção de notificações duplicadas a partir do cruzamento de gravidade e número de prontuário.  
* Criação estruturada de planos de ação diretos no sistema utilizando a metodologia 5W2H.  
* Rastreamento e atualização automática do status do incidente (Aberto, Em Análise, Concluído).

**Baixa Prioridade (Métricas e Relatórios)**

* Exportação de dados consolidados para suprir as planilhas de indicadores do hospital.  
* Organização de dados em formato de fácil integração com ferramentas externas de Business Intelligence (BI).

### **3\. Relatório de Validação do Sistema (e Protótipos)**

A validação do protótipo foi oficializada em reunião no dia 15 de maio de 2026, por videoconferência e presencialmente na Sala de Reuniões do HU, envolvendo a equipe de Desenvolvimento e o Setor de Qualidade do HU.

Os protótipos de tela apresentaram módulos de gestão de notificações, segurança/anonimato, análise de incidentes e coleta de indicadores. A partir das telas demonstradas, o cliente solicitou os seguintes ajustes e definiu algumas decisões:

| Tela/Módulo | Ajuste Solicitado / Decisão Tomada |
| :---- | :---- |
| **Tela de Triagem** | Foi pedida a inclusão de uma funcionalidade na interface que permita expandir os detalhes de cada notificação antes do início formal da triagem, garantindo uma visualização prévia rápida. A equipe de UI/UX ficou responsável pelo ajuste. |
| **Entrada de Dados (Indicadores)** | Ficou acordado que os formulários de indicadores exigirão salvamento intermediário e capacidade de reedição, pois a coleta ocorrerá em duas etapas distintas. Além disso, os dados que não vêm das notificações deverão ser preenchidos de forma manual. |

### 

### **4\. Critérios de Aceitação Definidos**

Todos os critérios do documento de planejamento foram revisados e **validados**. Eles balizam a entrega do escopo:

* **Abertura de Notificações via QR Code:** O sistema deverá encaminhar o usuário ao formulário exato baseado no QR Code lido. Desenvolvida em modelo *Mobile First*, a interface deve garantir que o carregamento e envio da notificação demorem, no máximo, 5 segundos. O sistema exigirá o preenchimento de pré-classificação, descrição do evento e prontuário (quando aplicável), além de permitir uploads de arquivos.  
* **Anonimato e Identificação:** A aplicação precisa garantir tecnicamente que envios anônimos sejam seguros. O campo de e-mail para comunicação de retornos deve ser mantido estritamente opcional.  
* **Triagem e Gestão (NSP):** O painel de triagem deve mostrar a gravidade e o prontuário logo na tela inicial, para que as duplicidades sejam notadas de cara. Analistas poderão reclassificar o incidente e utilizar filtros de busca avançada (data, gravidade, prontuário).  
* **Investigação e Plano de Ação:** O software deve tornar obrigatório o registro da causa raiz ao encerrar a investigação de um incidente. O fluxo de status precisará seguir as etapas cronológicas aprovadas: Aberto, Em Análise, Investigação da Causa Raiz, Plano de Ação e Concluído. Por envolver dados sensíveis, o acesso a esta etapa será restrito aos níveis autorizados da Qualidade e NSP.  
* **Coleta de Indicadores (Baixa Prioridade):** Para quando for implementado, os funcionários internos deverão preencher os formulários manualmente, com o sistema permitindo a consolidação e exportação nativa que alimentará as ferramentas de BI do hospital.

