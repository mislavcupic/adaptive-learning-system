import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';

export function MainLayout() {
    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950">
            <Sidebar />
            <div className="lg:ml-64">
                <Header />
                <main className="p-6">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
