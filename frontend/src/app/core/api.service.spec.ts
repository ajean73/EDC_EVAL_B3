import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('calls login endpoint with payload', () => {
    const payload = { email: 'john@pmt.local', password: 'secret-123' };

    service.login(payload).subscribe((response) => {
      expect(response.username).toBe('john');
    });

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush({ accountId: 3, username: 'john', email: payload.email });
  });

  it('calls workspaces endpoint with actorAccountId query param', () => {
    service.getWorkspaces(9).subscribe((response) => {
      expect(response).toHaveLength(1);
      expect(response[0].id).toBe(15);
    });

    const req = httpMock.expectOne((r) =>
      r.url === '/api/workspaces' && r.params.get('actorAccountId') === '9'
    );

    expect(req.request.method).toBe('GET');
    req.flush([{ id: 15, name: 'Projet', description: 'desc', startDate: '2026-08-01', ownerAccountId: 9, createdAt: null }]);
  });

  it('calls updateWorkItem endpoint with PATCH', () => {
    const payload = { status: 'DONE' as const, actorAccountId: 2, clearAssignee: true };

    service.updateWorkItem(1, 4, payload).subscribe((response) => {
      expect(response.status).toBe('DONE');
      expect(response.id).toBe(4);
    });

    const req = httpMock.expectOne('/api/workspaces/1/work-items/4');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(payload);

    req.flush({ id: 4, workspaceId: 1, title: 'Task', description: 'Desc', dueDate: null, priority: 'HIGH', status: 'DONE', creatorAccountId: 2, assignedAccountId: null, completedAt: '2026-08-01T10:00:00', createdAt: '2026-08-01T09:00:00', updatedAt: '2026-08-01T10:00:00' });
  });
});
