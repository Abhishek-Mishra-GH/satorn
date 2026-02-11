import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MoreHorizontal, Lock, Unlock, Shield, ShieldOff, Loader2 } from 'lucide-react';
import { User } from '@/shared/types';
import api from '@/shared/api/client';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";


export const UserManagementPage = () => {
    const queryClient = useQueryClient();

    const { data: users, isLoading } = useQuery({
        queryKey: ['admin-users'],
        queryFn: async () => {
            const res = await api.get<User[]>('/api/admin/users');
            return res.data;
        }
    });

    const toggleRoleMutation = useMutation({
        mutationFn: async ({ userId, role, action }: { userId: number, role: string, action: 'add' | 'remove' }) => {
            return api.put(`/api/admin/users/${userId}/roles/${action}?roleName=${role}`);
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    });

    const lockMutation = useMutation({
        mutationFn: async ({ userId, action }: { userId: number, action: 'lock' | 'unlock' }) => {
            return api.put(`/api/admin/users/${userId}/${action}`);
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    });

    const handleRoleToggle = (user: User, role: string) => {
        const hasRole = user.roles.includes(role);
        toggleRoleMutation.mutate({ 
            userId: user.id, 
            role, 
            action: hasRole ? 'remove' : 'add' 
        });
    };

    return (
        <div className="space-y-6">
             <div>
                <h1 className="text-3xl font-bold tracking-tight">User Management</h1>
                <p className="text-muted-foreground">Manage user access and roles.</p>
            </div>

            <div className="rounded-md border bg-card">
                 <table className="w-full text-sm">
                    <thead>
                        <tr className="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                            <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">ID</th>
                            <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">Username</th>
                            <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">Email</th>
                            <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">Roles</th>
                            <th className="h-12 px-4 text-right align-middle font-medium text-muted-foreground">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr><td colSpan={5} className="p-4 text-center"><Loader2 className="mx-auto h-6 w-6 animate-spin" /></td></tr>
                        ) : users?.map((user) => (
                            <tr key={user.id} className="border-b transition-colors hover:bg-muted/50">
                                <td className="p-4 align-middle">{user.id}</td>
                                <td className="p-4 align-middle font-medium">{user.username}</td>
                                <td className="p-4 align-middle text-muted-foreground">{user.email}</td>
                                <td className="p-4 align-middle">
                                    <div className="flex gap-1 flex-wrap">
                                        {user.roles.map(role => (
                                            <Badge key={role} variant="secondary" className="text-xs">
                                                {role.replace('ROLE_', '')}
                                            </Badge>
                                        ))}
                                    </div>
                                </td>
                                <td className="p-4 align-middle text-right">
                                     <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button variant="ghost" className="h-8 w-8 p-0">
                                                <MoreHorizontal className="h-4 w-4" />
                                            </Button>
                                        </DropdownMenuTrigger>
                                        <DropdownMenuContent align="end">
                                            <DropdownMenuLabel>Actions</DropdownMenuLabel>
                                            <DropdownMenuItem onClick={() => handleRoleToggle(user, 'ROLE_ADMIN')}>
                                                {user.roles.includes('ROLE_ADMIN') ? <ShieldOff className="mr-2 h-4 w-4" /> : <Shield className="mr-2 h-4 w-4" />}
                                                {user.roles.includes('ROLE_ADMIN') ? 'Remove Admin' : 'Make Admin'}
                                            </DropdownMenuItem>
                                             <DropdownMenuItem onClick={() => handleRoleToggle(user, 'ROLE_MODERATOR')}>
                                                {user.roles.includes('ROLE_MODERATOR') ? <ShieldOff className="mr-2 h-4 w-4" /> : <Shield className="mr-2 h-4 w-4" />}
                                                {user.roles.includes('ROLE_MODERATOR') ? 'Remove Mod' : 'Make Moderator'}
                                            </DropdownMenuItem>
                                            <DropdownMenuSeparator />
                                            {/* Note: User interface in frontend doesn't strictly have 'locked' field in prompt shared types, but assuming endpoint exists */}
                                            <DropdownMenuItem onClick={() => lockMutation.mutate({ userId: user.id, action: 'lock' })}>
                                                <Lock className="mr-2 h-4 w-4" /> Lock Account
                                            </DropdownMenuItem>
                                             <DropdownMenuItem onClick={() => lockMutation.mutate({ userId: user.id, action: 'unlock' })}>
                                                <Unlock className="mr-2 h-4 w-4" /> Unlock Account
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                     </DropdownMenu>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};
