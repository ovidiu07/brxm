/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.workflow;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

public class MyWorkflowImpl extends WorkflowImpl implements MyWorkflow
{
  //ArticleDocument article;
  int articleId;
  int authorId;
  public MyWorkflowImpl() throws RemoteException {
  }
  public void renameAuthor(String newName) throws WorkflowException, RepositoryException {
    AuthorDocument author = (AuthorDocument) context.getDocument("authors",newName);
    if(author == null)
      throw new WorkflowException("author does not exist");
    // article.authorId = author.authorId;
    authorId = author.authorId;
  }
}
