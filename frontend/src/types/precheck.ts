export interface PrecheckRequest {title:string;description:string;product?:string;module?:string;version?:string;severity?:string;impactScope?:string;attachmentsSummary?:string}
export interface ReferenceItem {sourceType:string;title:string;excerpt:string;url:string;mockData:boolean}
export interface PrecheckResponse {precheckId:string;summary:string;recommendations:string[];references:ReferenceItem[];confidence:'HIGH'|'MEDIUM'|'LOW';humanReviewRequired:boolean;missingInformation:string[];fallbackReason:string}
